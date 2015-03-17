package com.google.gwt.sample.stockwatcher.server;

import com.google.gwt.sample.stockwatcher.client.StockPrice2;
import com.google.gwt.sample.stockwatcher.client.StockPriceService;
import com.google.gwt.sample.stockwatcher.client.DelistedException;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import java.util.Random;

public class StockPriceServiceImpl extends RemoteServiceServlet implements
	StockPriceService {
	private static final double MAX_PRICE = 100.0; // $100.0
	private static final double MAX_PRICE_CHANGE = 0.02; // +/- 2%

	public StockPrice2[] getPrices(String[] symbols) throws DelistedException {
		Random rnd = new Random();

		StockPrice2[] prices = new StockPrice2[symbols.length];
		for(int i=0; i < symbols.length; i++){
			if(symbols[i].equals("ERR")){
				throw new DelistedException("ERR");
			}

			double price = rnd.nextDouble() * MAX_PRICE;
			double change = price * MAX_PRICE_CHANGE * (rnd.nextDouble() * 2f - 1f);

			prices[i] = new StockPrice2(symbols[i], price, change);
		}
		
		return prices;
	}
}