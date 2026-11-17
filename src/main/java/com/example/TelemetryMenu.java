package com.example;

import bluej.extensions2.BPackage;
import bluej.extensions2.MenuGenerator;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;

import java.nio.file.Path;
import java.util.List;

// Adds "Export telemetry now" to BlueJ's Tools menu.
public class TelemetryMenu extends MenuGenerator {

    private final TelemetryListener listener;

    public TelemetryMenu(TelemetryListener listener) {
        this.listener = listener;
    }

    @Override
    public MenuItem getToolsMenuItem(BPackage pkg) {
        MenuItem item = new MenuItem("Export telemetry now");
        item.setOnAction(e -> exportAndShowFiles());
        return item;
    }

    private void exportAndShowFiles() {
        List<Path> files = listener.exportSnapshot();

        String message = "Nothing has been recorded yet.";
        if (!files.isEmpty()) {
            message = "Saved:";
            for (Path file : files) {
                message += "\n" + file;
            }
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Telemetry export");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
