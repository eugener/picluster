package com.gluonhq.iotmonitor.monitor;

/*
 * Copyright (c) 2019 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.chart.ChartData;
import eu.hansolo.tilesfx.events.ChartDataEventListener;
import eu.hansolo.tilesfx.fonts.Fonts;
import eu.hansolo.tilesfx.skins.TileSkin;
import eu.hansolo.tilesfx.tools.CtxBounds;
import eu.hansolo.tilesfx.tools.Helper;
import javafx.beans.InvalidationListener;
import javafx.collections.WeakListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class CpuMemTileSkin extends TileSkin {
    private Text                      titleText;
    private Text                      text;
    private VBox                      chartPane;
    private ChartDataEventListener    updateHandler;
    private InvalidationListener      paneSizeListener;
    private Map<ChartData, ChartItem> dataItemMap;


    // ******************** Constructors **************************************
    public CpuMemTileSkin(final Tile TILE) {
        super(TILE);
    }


    // ******************** Initialization ************************************
    @Override protected void initGraphics() {
        super.initGraphics();

        updateHandler    = e -> {
            switch(e.getType()) {
                case UPDATE  : break;
                case FINISHED: updateChart();
            }
        };
        paneSizeListener = e -> updateChart();
        dataItemMap = new HashMap<>();

        chartPane = new VBox();

        tile.getChartData().forEach(data -> {
            data.addChartDataEventListener(updateHandler);
            dataItemMap.put(data, new ChartItem(data, contentBounds));
        });
        chartPane.getChildren().setAll(dataItemMap.values());

        titleText = new Text();
        titleText.setFill(tile.getTitleColor());
        Helper.enableNode(titleText, !tile.getTitle().isEmpty());

        text = new Text(tile.getText());
        text.setFill(tile.getUnitColor());
        Helper.enableNode(text, tile.isTextVisible());

        getPane().getChildren().addAll(titleText, text, chartPane);
    }

    @Override protected void registerListeners() {
        super.registerListeners();
        tile.getChartData().addListener(new WeakListChangeListener<>(change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    change.getAddedSubList().forEach(addedData -> {
                        addedData.addChartDataEventListener(updateHandler);
                        dataItemMap.put(addedData, new ChartItem(addedData, contentBounds));
                    });
                    chartPane.getChildren().setAll(dataItemMap.values());
                    updateChart();
                } else if (change.wasRemoved()) {
                    change.getRemoved().forEach(removedData -> {
                        removedData.removeChartDataEventListener(updateHandler);
                        dataItemMap.remove(removedData);
                    });
                    chartPane.getChildren().setAll(dataItemMap.values());
                    updateChart();
                }
            }
        }));

        pane.widthProperty().addListener(paneSizeListener);
        pane.heightProperty().addListener(paneSizeListener);
    }


    // ******************** Methods *******************************************
    @Override protected void handleEvents(final String EVENT_TYPE) {
        super.handleEvents(EVENT_TYPE);

        if ("VISIBILITY".equals(EVENT_TYPE)) {
            Helper.enableNode(titleText, !tile.getTitle().isEmpty());
            Helper.enableNode(text, tile.isTextVisible());
        } else if ("DATA".equals(EVENT_TYPE)) {
            updateChart();
        }
    }

    @Override public void dispose() {
        pane.widthProperty().removeListener(paneSizeListener);
        pane.heightProperty().removeListener(paneSizeListener);
        tile.getBarChartItems().forEach(item -> item.removeChartDataEventListener(updateHandler));
        dataItemMap.clear();
        super.dispose();
    }


    // ******************** Resizing ******************************************
    @Override protected void resizeStaticText() {
        double maxWidth = width - size * 0.1;
        double fontSize = size * textSize.factor;

        boolean customFontEnabled = tile.isCustomFontEnabled();
        Font    customFont        = tile.getCustomFont();
        Font    font              = (customFontEnabled && customFont != null) ? Font.font(customFont.getFamily(), fontSize) : Fonts.latoRegular(fontSize);

        titleText.setFont(font);
        if (titleText.getLayoutBounds().getWidth() > maxWidth) { Helper.adjustTextSize(titleText, maxWidth, fontSize); }
        switch(tile.getTitleAlignment()) {
            default    :
            case LEFT  : titleText.relocate(size * 0.05, size * 0.05); break;
            case CENTER: titleText.relocate((width - titleText.getLayoutBounds().getWidth()) * 0.5, size * 0.05); break;
            case RIGHT : titleText.relocate(width - (size * 0.05) - titleText.getLayoutBounds().getWidth(), size * 0.05); break;
        }

        text.setText(tile.getText());
        text.setFont(font);
        if (text.getLayoutBounds().getWidth() > maxWidth) { Helper.adjustTextSize(text, maxWidth, fontSize); }
        switch(tile.getTextAlignment()) {
            default    :
            case LEFT  : text.setX(size * 0.05); break;
            case CENTER: text.setX((width - text.getLayoutBounds().getWidth()) * 0.5); break;
            case RIGHT : text.setX(width - (size * 0.05) - text.getLayoutBounds().getWidth()); break;
        }
        text.setY(height - size * 0.05);
    }

    @Override protected void resize() {
        super.resize();

        chartPane.setPrefSize(width * 0.8, contentBounds.getHeight());
        chartPane.relocate(contentBounds.getX(), contentBounds.getY());
        chartPane.setSpacing(contentBounds.getHeight() * 0.25);

        double itemHeight = contentBounds.getHeight() * 0.725 / (dataItemMap.size());

        dataItemMap.values().forEach(item -> {
            item.setPrefSize(contentBounds.getWidth(), itemHeight);
            item.setLayoutX(contentBounds.getX());
        });

        if (titleText.getText().isEmpty()) {
            chartPane.setSpacing((contentBounds.getHeight() - (2 * itemHeight)));
        } else {
            chartPane.setSpacing((contentBounds.getHeight() - (2 * itemHeight)) / 1.5);
        }

        updateChart();
    }

    @Override protected void redraw() {
        super.redraw();
        titleText.setText(tile.getTitle());
        text.setText(tile.getText());

        resizeDynamicText();
        resizeStaticText();

        titleText.setFill(tile.getTitleColor());
        text.setFill(tile.getTextColor());
    }

    private void updateChart() {
        int    noOfItems   = dataItemMap.size();
        if (noOfItems == 0) return;
        for (int i = 0 ; i < noOfItems ; i++) {
            ChartData item = dataItemMap.keySet().iterator().next();
            dataItemMap.get(item).update();

            if (i > 1) { break; }
        }
    }


    // ******************** Internal Classes **********************************
    private class ChartItem extends Region {
        private static final double                 PREF_WIDTH  = 330;
        private static final double                 PREF_HEIGHT = 95;
        private              ChartData              chartData;
        private              CtxBounds              contentBounds;
        private              Label                  title;
        private              Label                  value;
        private              Rectangle              scale;
        private              Rectangle              bar;
        private              String                 formatString;
        private              double                 step;
        private              ChartDataEventListener chartDataListener;


        public ChartItem(final ChartData CHART_DATA, final CtxBounds CONTENT_BOUNDS) {
            chartData         = CHART_DATA;
            contentBounds     = CONTENT_BOUNDS;
            title             = new Label(chartData.getName());
            value             = new Label(String.format(Locale.US, "%.0f%%", chartData.getValue()));
            scale             = new Rectangle(0, 0);
            bar               = new Rectangle(0, 0);
            formatString      = "%.0f%%";
            step              = PREF_WIDTH / maxValue;
            chartDataListener = e -> {
                switch(e.getType()) {
                    case UPDATE  : bar.setFill(chartData.getFillColor()); break;
                    case FINISHED: update();
                }
            };
            initGraphics();
            registerListeners();
        }


        private void initGraphics() {
            setPrefSize(contentBounds.getWidth(), contentBounds.getHeight() * 0.2375);
            Font font = Fonts.latoRegular(Helper.clamp(1, 48, getPrefHeight() * 0.50526316));
            title.setFont(font);
            title.setTextFill(Color.rgb(238, 238, 238));
            title.setAlignment(Pos.CENTER_LEFT);

            value.setFont(font);
            value.setTextFill(Color.rgb(238, 238, 238));
            value.setAlignment(Pos.CENTER_RIGHT);
            scale.setFill(Color.rgb(90, 90, 90));
            bar.setFill(chartData.getFillColor());

            getChildren().addAll(title, value, scale, bar);
        }

        private void registerListeners() {
            chartData.addChartDataEventListener(chartDataListener);
            widthProperty().addListener(o -> resize());
            heightProperty().addListener(o -> resize());
        }

        public String getFormatString() { return formatString; }
        public void setFormatString(final String FORMAT_STRING) {
            formatString = FORMAT_STRING;
            update();
        }

        public void update() {
            value.setText(String.format(Locale.US, formatString, chartData.getValue()));
            bar.setWidth(chartData.getValue() * step);
        }

        public void dispose() {
            chartData.removeChartDataEventListener(chartDataListener);
        }

        private void resize() {
            double width  = getPrefWidth();
            double height = getPrefHeight();

            step = width / 100.0;

            double textWidth  = width * 0.5;
            double textHeight = height * 0.13;

            title.setPrefSize(textWidth, textHeight);
            value.setPrefSize(textWidth, textHeight);

            Font font = Fonts.latoRegular(Helper.clamp(1, 48, getPrefHeight() * 0.50526316));
            title.setFont(font);
            value.setFont(font);

            title.setLayoutX(0);
            title.setLayoutY(0);

            value.setLayoutX(width * 0.5);
            value.setLayoutY(0);

            bar.setX(0);
            bar.setY(title.getLayoutY() + title.getFont().getSize() + height * 0.12);
            bar.setHeight(height * 0.52631579);
            bar.setWidth(chartData.getValue() * step);

            scale.setX(0);
            scale.setWidth(width);
            scale.setHeight(height * 0.05263158);
            scale.setY(bar.getY() + (bar.getHeight() - scale.getHeight()) * 0.5);

            update();
        }
    }
}

