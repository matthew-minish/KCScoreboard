package scoreboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class Controller<T extends Parent> implements Initializable {

    private static final String SAVE_FILE_NAME = "teamScores.txt";

    @FXML
    private BarChart<String, Number> barChart;

    @FXML
    private CategoryAxis teamAxis;

    @FXML
    private NumberAxis pointAxis;

    private XYChart.Series teamData;
    private String[] teams = {"Blue Boys", "Green Boys", "Orange Boys", "Red Boys", "Yellow Boys", "Blue Girls", "Green Girls", "Orange Girls", "Red Girls", "Yellow Girls"};

    /**
     * Creates and appends all bar chart axes, then adds all teams
     */
    private void createBarChartAxes() {
        barChart.getData().add(teamData);
        try {
            // Create data series for boys and girls
            for (String team : teams) {
                teamData.getData().add(new XYChart.Data(team, readScoreFromFile(team)));
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

        for (XYChart.Data data : (ObservableList<XYChart.Data>)teamData.getData()) {
            node.put(teams[teamData.getData().indexOf(data)], (int)data.getYValue());
        }

        String str = mapper.writeValueAsString(node);

        BufferedWriter writer = new BufferedWriter(new FileWriter(SAVE_FILE_NAME));
        writer.write(str);

        writer.close();
    }

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
        teamData = new XYChart.Series();
        barChart.setData(FXCollections.observableArrayList());
        createBarChartAxes();
    }
}
