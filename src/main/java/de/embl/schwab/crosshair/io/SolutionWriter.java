package de.embl.schwab.crosshair.io;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;

public class SolutionWriter {

    private Solution solution;
    private String filePath;

    public SolutionWriter( Solution solution, String filePath ) {
        this.solution = solution;
        this.filePath = filePath;
    }

    public void writeSolution() {
        try {
            FileWriter fileWriter = new FileWriter(filePath);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson( solution, fileWriter);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

}
