package scoreboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class Controller implements Initializable {

    private static final String SAVE_FILE_NAME = "teamScores.txt";

    @FXML
    private BarChart<String, Number> barChart;

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
        // Create bars for all teams
        teamData = new XYChart.Series<>();
        barChart.setData(FXCollections.observableArrayList());
        createBarChartAxes();
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
        StringBuilder contentBuilder = new StringBuilder();
        try(Stream<String> stream = Files.lines( Paths.get(SAVE_FILE_NAME), StandardCharsets.UTF_8)) {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            return 0;
        }

        ObjectMapper mapper = new ObjectMapper();

        JsonNode node = mapper.readTree(contentBuilder.toString());
        return node.get(team).asInt(-1);
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
        dataText.setFont(new Font(50));
        dataText.getStyleClass().add("outline");

        node.parentProperty().addListener((ov, oldParent, parent) -> {
            Group parentGroup = (Group) parent;
            parentGroup.getChildren().add(dataText);
        });

        // Calculate bounds and set position
        node.boundsInParentProperty().addListener((ov, oldBounds, bounds) -> {
            long yPosition = Math.round(
                    bounds.getMinY()
                    + (bounds.getMaxY() - bounds.getMinY()) / 2
            );


            dataText.setLayoutX(
                Math.round(bounds.getMinX() + bounds.getWidth() / 2 - dataText.getLayoutBounds().getWidth() / 2)
            );
            dataText.setLayoutY(yPosition);
        });
    }
}
