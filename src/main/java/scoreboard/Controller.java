package scoreboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.util.*;

public class Controller implements Initializable {

    private static final String SAVE_FILE_NAME = "teamScores.json";
    private static final String CONFIG_FILE_NAME = "config.json";
    private static final String CHANGES_FILE_NAME = "changesFromAPI.json";
    private static final String DEFAULT_FONT_FAMILY = Font.getDefault().getFamily();

    private Configuration config;

    @FXML
    private BarChart<String, Number> barChart;

    @FXML
    private ComboBox<String> fontFamilySelector;

    @FXML
    private Button plusFontSizeButton, minusFontSizeButton, plusFontOutlineButton, minusFontOutlineButton;
    private ObjectProperty<Font> fontProperty;

    @FXML
    private GridPane settingsGridPane;

    @FXML
    private CheckBox showSettingsCheckbox;

    @FXML
    private CheckBox listenForNetworkChangesCheckbox;

    @FXML
    private Text ipAddressLabel;

    private List<Text> textLabels = new ArrayList<>();

    private XYChart.Series<String, Number> teamData;
    private String[] teams = {"Blue Boys", "Green Boys", "Orange Boys", "Red Boys", "Yellow Boys", "Blue Girls", "Green Girls", "Orange Girls", "Red Girls", "Yellow Girls"};

    private StringProperty[] stringProps = new SimpleStringProperty[] {
            new SimpleStringProperty(""),
            new SimpleStringProperty(""),
            new SimpleStringProperty(""),
            new SimpleStringProperty(""),
            new SimpleStringProperty(""),
            new SimpleStringProperty(""),
            new SimpleStringProperty(""),
            new SimpleStringProperty(""),
            new SimpleStringProperty(""),
            new SimpleStringProperty(""),
    };

    /**
     * Called to initialize a controller after its root element has been
     * completely processed.
     *
     * @param location  The location used to resolve relative paths for the root object, or
     *                  <tt>null</tt> if the location is not known.
     * @param resources The resources used to localize the root object, or <tt>null</tt> if
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Load config file if one exists
        readConfigFile();
        fontProperty = new SimpleObjectProperty<>(new Font(config.getSelectedFontFamily(), config.getFontSize()));
        listenForNetworkChangesCheckbox.setSelected(config.isListeningForNetworkChanges());
        refreshIPLabel();

        // Create bars for all teams
        teamData = new XYChart.Series<>();
        barChart.setData(FXCollections.observableArrayList());
        createBarChartAxes();

        config.addObserver((o, arg) -> {
            writeConfigFile();
        });

        fontFamilySelector.itemsProperty().setValue(FXCollections.observableArrayList(javafx.scene.text.Font.getFamilies()));
        fontFamilySelector.getSelectionModel().select(config.getSelectedFontFamily());
        settingsGridPane.visibleProperty().bind(showSettingsCheckbox.selectedProperty());

        plusFontSizeButton.setOnMouseClicked(event -> {
            config.setFontSize(config.getFontSize() * 1.075);
            fontProperty.setValue(new Font(config.getSelectedFontFamily(), config.getFontSize()));
        });

        minusFontSizeButton.setOnMouseClicked(event -> {
            config.setFontSize(config.getFontSize() * 0.925);
            fontProperty.setValue(new Font(config.getSelectedFontFamily(), config.getFontSize()));
        });

        plusFontOutlineButton.setOnMouseClicked(event -> {
            config.setFontOutlineProportion(config.getFontOutlineProportion() * 1.075);
            refreshAllFontOutlines();
        });

        minusFontOutlineButton.setOnMouseClicked(event -> {
            config.setFontOutlineProportion(config.getFontOutlineProportion() * 0.925);
            refreshAllFontOutlines();
        });

        fontFamilySelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            config.setSelectedFontFamily(newValue);
            fontProperty.setValue(new Font(config.getSelectedFontFamily(), config.getFontSize()));
        });

        listenForNetworkChangesCheckbox.selectedProperty().addListener(((observable, oldValue, newValue) -> {
            config.setListeningForNetworkChanges(newValue);
            refreshIPLabel();
        }));

        createCheckForNetworkChangesTask();
    }

    private void refreshIPLabel(){
        if(!config.isListeningForNetworkChanges()) {
            ipAddressLabel.setText("Not listening on network");
        } else {
            try(final DatagramSocket socket = new DatagramSocket()){
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                String ip = socket.getLocalAddress().getHostAddress();
                // This has the very bold assumption that the accompanying web API is running on port 15000 and uses the following endpoint URL
                ipAddressLabel.setText(ip + ":15000/changepoints");
            } catch (SocketException e) {
                ipAddressLabel.setText("Socket error");
            } catch (UnknownHostException e) {
                ipAddressLabel.setText("Can't get host IP");
            }
        }
    }

    private void createCheckForNetworkChangesTask() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handleScoreChangesFromFile();
            }
        }, 1000, 1000);
    }

    private void readConfigFile() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            config = mapper.readValue(new File(CONFIG_FILE_NAME), Configuration.class);
        } catch (Exception e) {
            config = new Configuration(50.0, 0.03, DEFAULT_FONT_FAMILY, false);
        }
    }

    private void writeConfigFile() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(new File(CONFIG_FILE_NAME), config);
        } catch (Exception e) {
            System.out.println("Failed to write to file");
        }
    }

    /**
     * Creates and appends all bar chart axes, then adds all teams
     */
    private void createBarChartAxes() {
        barChart.getData().add(teamData);
        try {
            // Create data series for boys and girls
            for (int i = 0; i < teams.length; i++) {
                final XYChart.Data<String, Number> data = new XYChart.Data<>(teams[i], readScoreFromFile(teams[i]));
                data.nodeProperty().addListener((ov, oldNode, node) -> {
                    if (node != null) {
                        displayLabelForData(data);
                    }
                });
                stringProps[i].setValue(data.getYValue() + "");
                teamData.getData().add(data);
            }
        } catch (IOException e) {
            System.out.println("Failed to read file data");
        }
    }

    /**
     * Reads the score for a particular team from specified save file
     * @param team Name of team
     */
    private int readScoreFromFile(String team) throws IOException {
        JsonNode node = readFileToNode(SAVE_FILE_NAME);
        return node.get(team).asInt(-1);
    }

    private JsonNode readFileToNode(String filename) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(Paths.get(filename).toFile());
    }

    /**
     * Check for any changes in the file that is modified by API to receive network queries
     */
    private void handleScoreChangesFromFile() {
        if (!config.isListeningForNetworkChanges()) return;
        try {
            JsonNode tree = readFileToNode(CHANGES_FILE_NAME);
            if(tree == null) return;
            tree.fields().forEachRemaining(node -> {
                if (Arrays.asList(teams).contains(node.getKey())) {
                    int changeAmount = node.getValue().asInt(0);
                    modifyPoints(node.getKey(), changeAmount);
                }
            });
            PrintWriter writer = new PrintWriter(new File(CHANGES_FILE_NAME));
            writer.print("");
            writer.close();
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    /**
     * Writes all current scores to a file
     */
    private void writeScoresToFile() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();

        for (XYChart.Data data : teamData.getData()) {
            node.put(teams[teamData.getData().indexOf(data)], (int)data.getYValue());
        }

        String str = mapper.writeValueAsString(node);

        BufferedWriter writer = new BufferedWriter(new FileWriter(SAVE_FILE_NAME));
        writer.write(str);

        writer.close();
    }

    /**
     * Method to modify the points of some team by some amount
     *
     * @param team Name of team to modify points of
     * @param change Amount to modify points by (can be negative to remove points)
     */
    private void modifyPoints(String team, int change) {
        for (XYChart.Data<String, Number> data : teamData.getData()) {
            if(data.getXValue().equals(team)) {
                data.setYValue((int)data.getYValue() + change);
                displayLabelForData(data);
                stringProps[teamData.getData().indexOf(data)].setValue(data.getYValue() + "");
                try {
                    writeScoresToFile();
                } catch (IOException e) {
                    System.out.println("Failed to write to file");
                }
            }
        }
    }

    /**
     * FXML injected method to handle adding (or removing) points based on the button that was pressed
     * @param e The event created by the button click
     */
    @FXML
    private void changeTen(ActionEvent e) {
        Button button = (Button)e.getSource();
        boolean adding = button.getText().startsWith("+");
        boolean isBoys = button.idProperty().get().charAt(1) == 'b';

        String colour = "";
        switch (button.idProperty().get().charAt(0)) {
            case 'b':
                colour = "Blue";
                break;
            case 'g':
                colour = "Green";
                break;
            case 'o':
                colour = "Orange";
                break;
            case 'r':
                colour = "Red";
                break;
            case 'y':
                colour = "Yellow";
                break;
        }

        modifyPoints(colour + " " + (isBoys ? "Boys" : "Girls"), adding ? 10 : -10);
    }

    /**
     * Dynamically creates and positions a label above the bar representing the data point
     * Credit: https://stackoverflow.com/a/15375168
     *
     * @param data Data point to display value for
     */
    private void displayLabelForData(XYChart.Data<String, Number> data) {
        final Node node = data.getNode();
        final Text dataText = new Text(data.getYValue() + "");
        dataText.textProperty().bind(stringProps[teamData.getData().indexOf(data)]);
        dataText.fontProperty().bind(fontProperty);
        dataText.getStyleClass().add("outline");

        node.parentProperty().addListener((ov, oldParent, parent) -> {
            Group parentGroup = (Group) parent;
            parentGroup.getChildren().add(dataText);
        });

        // Calculate bounds and set position
        node.boundsInParentProperty().addListener((ov, oldBounds, bounds) -> {
            updateLabelBounds(dataText, bounds);
            refreshFontOutline(dataText);
        });
        dataText.fontProperty().addListener((observable, oldValue, newValue) -> {
            updateLabelBounds(dataText, node.getBoundsInParent());
            refreshFontOutline(dataText);
        });

        textLabels.add(dataText);
    }

    private void updateLabelBounds(Text dataText, Bounds bounds) {
        long yPosition = Math.round(
                bounds.getMinY()
                        + (bounds.getMaxY() - bounds.getMinY()) / 2
        );

        dataText.setLayoutX(
                Math.round(bounds.getMinX() + bounds.getWidth() / 2 - dataText.getLayoutBounds().getWidth() / 2)
        );
        dataText.setLayoutY(yPosition);
    }

    private void refreshFontOutline(Text dataText) {
        dataText.setStyle(String.format("-fx-stroke-width: %f;", config.getFontSize() * config.getFontOutlineProportion()));
    }

    private void refreshAllFontOutlines() {
        for (Text dataText : textLabels) {
            refreshFontOutline(dataText);
        }
    }
}
