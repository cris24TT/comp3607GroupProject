package com.example;

import bluej.extensions2.BPackage;
import bluej.extensions2.MenuGenerator;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuItem;

import java.nio.file.Path;
import java.util.List;

/** Adds Tools > "Export telemetry now" to BlueJ. */
public class TelemetryMenu extends MenuGenerator {

    private final TelemetryListener listener;

    public TelemetryMenu(TelemetryListener listener) {
        this.listener = listener;
    }

    @Override
    public MenuItem getToolsMenuItem(BPackage pkg) {
        MenuItem item = new MenuItem("Export telemetry now");
        item.setOnAction(e -> {
            List<Path> files = listener.exportSnapshot();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Telemetry export");
            alert.setHeaderText(null);
            alert.setContentText(files.isEmpty()
                    ? "Nothing has been recorded yet."
                    : "Saved:\n" + String.join("\n", files.stream().map(Path::toString).toList()));
            alert.show();
        });
        return item;
    }
}
