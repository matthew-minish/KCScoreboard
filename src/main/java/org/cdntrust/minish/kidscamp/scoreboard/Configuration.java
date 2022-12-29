package org.cdntrust.minish.kidscamp.scoreboard;

import java.util.Observable;

public class Configuration extends Observable {
    private double fontSize;
    private double fontOutlineProportion;
    private String selectedFontFamily;
    private boolean listeningForNetworkChanges;

    public Configuration() {}

    public Configuration(double fontSize, double fontOutlineProportion, String selectedFontFamily, boolean listeningForNetworkChanges) {
        this.fontSize = fontSize;
        this.fontOutlineProportion = fontOutlineProportion;
        this.selectedFontFamily = selectedFontFamily;
        this.listeningForNetworkChanges = listeningForNetworkChanges;
    }

    public double getFontSize() {
        return fontSize;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = fontSize;
        this.setChanged();
        this.notifyObservers();
    }

    public double getFontOutlineProportion() {
        return fontOutlineProportion;
    }

    public void setFontOutlineProportion(double fontOutlineProportion) {
        this.fontOutlineProportion = fontOutlineProportion;
        this.setChanged();
        this.notifyObservers();
    }

    public String getSelectedFontFamily() {
        return selectedFontFamily;
    }

    public void setSelectedFontFamily(String selectedFontFamily) {
        this.selectedFontFamily = selectedFontFamily;
        this.setChanged();
        this.notifyObservers();
    }

    public boolean isListeningForNetworkChanges() {
        return listeningForNetworkChanges;
    }

    public void setListeningForNetworkChanges(boolean listeningForNetworkChanges) {
        this.listeningForNetworkChanges = listeningForNetworkChanges;
        this.setChanged();
        this.notifyObservers();
    }
}
