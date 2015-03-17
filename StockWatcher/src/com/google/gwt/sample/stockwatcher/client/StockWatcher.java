package com.google.gwt.sample.stockwatcher.client;

import com.google.gwt.sample.stockwatcher.shared.FieldVerifier;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.Window;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Random;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.i18n.client.DateTimeFormat;
import java.util.Date;


import com.google.gwt.user.client.ui.RootPanel;
import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class StockWatcher implements EntryPoint {
  private static final int REFRESH_INTERVAL = 5000; //ms
  private VerticalPanel mainPanel = new VerticalPanel();
  private FlexTable stocksFlexTable = new FlexTable();
  private HorizontalPanel addPanel = new HorizontalPanel();
  private TextBox newSymbolTextBox = new TextBox();
  private Button addStockButton = new Button("Add");
  private Label lastUpdatedLabel = new Label();
  private ArrayList<String> stocks = new ArrayList<String>();
  private StockPriceServiceAsync stockPriceSvc = GWT.create(StockPriceService.class);
  private Label errorMsgLabel = new Label();


  /**
   * The message displayed to the user when the server cannot be reached or
   * returns an error.
   */
  private static final String SERVER_ERROR = "An error occurred while "
      + "attempting to contact the server. Please check your network "
      + "connection and try again.";

  /**
   * Create a remote service proxy to talk to the server-side Greeting service.
   */
  private final GreetingServiceAsync greetingService = GWT.create(GreetingService.class);

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    //create table for stock data
    stocksFlexTable.setText(0, 0, "Symbol");
    stocksFlexTable.setText(0, 1, "Price");
    stocksFlexTable.setText(0, 2, "Change");
    stocksFlexTable.setText(0, 3, "Remove");

    //Add styles to elements in the stock list table
    stocksFlexTable.getRowFormatter().addStyleName(0, "watchListHeader");
    stocksFlexTable.addStyleName("watchList");
    stocksFlexTable.getCellFormatter().addStyleName(0, 1, "watchListNumericColumn");
    stocksFlexTable.getCellFormatter().addStyleName(0, 2, "watchListNumericColumn");
    stocksFlexTable.getCellFormatter().addStyleName(0, 3, "watchListRemoveColumn");

    //Assemble Add Stock panel
    addPanel.add(newSymbolTextBox);
    addPanel.add(addStockButton);
    addPanel.addStyleName("addPanel");
    errorMsgLabel.setStyleName("errorMessage");
    errorMsgLabel.setVisible(false);

    //Assemble Main panel
    mainPanel.add(errorMsgLabel);
    mainPanel.add(stocksFlexTable);
    mainPanel.add(addPanel);
    mainPanel.add(lastUpdatedLabel);

    //Associate the Main panel with the HTML host page
    RootPanel.get("stockList").add(mainPanel);

    //Move cursor focus to the input box
    newSymbolTextBox.setFocus(true);

    //Setup timer to refresh list automatically
    Timer refreshTimer = new Timer() {
        @Override
        public void run(){
            refreshWatchList();
        }
    };

    refreshTimer.scheduleRepeating(REFRESH_INTERVAL);

    //Listen for mouse events on the add button
    addStockButton.addClickHandler(new ClickHandler(){
        public void onClick(ClickEvent event){
            addStock();
        }
    });

    //Listen for keyboard events in the input box
    newSymbolTextBox.addKeyDownHandler(new KeyDownHandler() {
        public void onKeyDown(KeyDownEvent event){
            if(event.getNativeKeyCode() == KeyCodes.KEY_ENTER){
                addStock();
            }
        }
    });
  }

      /** 
    * Add Stock to flexTable. Executed when the user clicks the addStockButton or 
    * presses enter in the newSymbolTextBox.
    */
    private void addStock() {
        final String symbol = newSymbolTextBox.getText().toUpperCase().trim();
        newSymbolTextBox.setFocus(true);

        //Stock code must be between 1 an 10 chars that are numbers, letters, or dots
        if(!symbol.matches("^[0-9A-Z&#92;&#92;.]{1,10}$")){
            Window.alert("'" + symbol + "'is not a valid symbol");
            newSymbolTextBox.selectAll();
            return;
        }

        newSymbolTextBox.setText("");

        //Don't add stock if it's already in the table
        if(stocks.contains(symbol))
            return;
        //Add the stock to the table
        int row = stocksFlexTable.getRowCount();
        stocks.add(symbol);
        stocksFlexTable.setText(row, 0, symbol);

        //Add a button to remove this stock from the table
        Button removeStockButton = new Button("x");
        removeStockButton.addStyleDependentName("remove");
        stocksFlexTable.setWidget(row, 2, new Label());
        stocksFlexTable.getCellFormatter().addStyleName(row, 1, "watchListNumericColumn");
        stocksFlexTable.getCellFormatter().addStyleName(row, 2, "watchListNumericColumn");
        stocksFlexTable.getCellFormatter().addStyleName(row, 2, "watchListRemoveColumn");

        removeStockButton.addClickHandler(new ClickHandler(){
            public void onClick(ClickEvent event){
                int removeIndex = stocks.indexOf(symbol);
                stocksFlexTable.removeRow(removeIndex + 1);
            }
        });
        stocksFlexTable.setWidget(row, 3, removeStockButton);

        //Get the stock price
        refreshWatchList();
    }

    private void refreshWatchList(){
        // final double MAX_PRICE = 100.0; //100.O$
        // final double MAX_PRICE_CHANGE = 0.02;
        //Initialize the service proxy
        if(stockPriceSvc == null) {
            stockPriceSvc = GWT.create(StockPriceService.class);
        }

        //setup the callback object
        AsyncCallback<StockPrice2[]> callback = new AsyncCallback<StockPrice2[]>(){
            public void onFailure(Throwable caught){
                //If the stock code is in the list of delisted codes, display en error message
                String details = caught.getMessage();
                if(caught instanceof DelistedException) {
                    details = "company '"+((DelistedException) caught).getSymbol() + "' was delisted";
                } 

                errorMsgLabel.setText("Error: " + details);
                errorMsgLabel.setVisible(true);
            }

            public void onSuccess(StockPrice2[] result) {
                updateTable(result);
            }
        };

        //Make the call to the stock price service
        stockPriceSvc.getPrices(stocks.toArray(new String[0]), callback);

    }

    private void updateTable(StockPrice2[] prices) {
        for(int i = 0; i < prices.length; i++){
            updateTable(prices[i]);
        }

        //Display timestamp showing last refresh.
        lastUpdatedLabel.setText("Last update : " + 
            DateTimeFormat.getMediumDateTimeFormat().format(new Date()));

        //clean any errors
        errorMsgLabel.setVisible(false);
    }

    /** 
    * Update a single row in the stock table
    *
    * @param price Stock data for a single row
    */
    private void updateTable(StockPrice2 price){
        //Make sure the stock is full in the stock table
        if(!stocks.contains(price.getSymbol())){
            return;
        }

        int row = stocks.indexOf(price.getSymbol()) + 1;

        //Format the data in the price and change fields
        String priceText = NumberFormat.getFormat("#,##0.00").format(price.getPrice());
        NumberFormat changeFormat = NumberFormat.getFormat("+#,##0.00;-#,##0.00");
        String changeText = changeFormat.format(price.getChange());
        String changePercentText = changeFormat.format(price.getChangePercent());

        //Populate the price and change fields with new data
        stocksFlexTable.setText(row, 1, priceText);
        // <span class="strike">stocksFlexTable.setText(row, 2, changeText + " (" +
        //     changePercentText + "%)");

        Label changeWidget = (Label) stocksFlexTable.getWidget(row, 2);
        changeWidget.setText(changeText + " (" + changePercentText + "%)");
        // stocksFlexTable.setText(row, 2, changeText + "(" + changePercentText + "%)");

        //Change the color of text in the change field based on its value
        String changeStyleName = "noChange";
        if(price.getChangePercent() < -0.1f){
            changeStyleName = "negativeChange";
        } else if (price.getChangePercent() > 0.1f) {
            changeStyleName = "positiveChange";
        }

        changeWidget.setStyleName(changeStyleName);
    }
}
