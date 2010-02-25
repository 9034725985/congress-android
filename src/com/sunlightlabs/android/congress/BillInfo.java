package com.sunlightlabs.android.congress;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.app.ListActivity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.commonsware.cwac.merge.MergeAdapter;
import com.sunlightlabs.android.congress.utils.Utils;
import com.sunlightlabs.android.congress.utils.ViewArrayAdapter;
import com.sunlightlabs.congress.java.Bill;
import com.sunlightlabs.congress.java.CongressException;
import com.sunlightlabs.congress.java.Legislator;

public class BillInfo extends ListActivity {
	private String id, code, title;
	private long introduced_at;
	
	private Bill bill;
	private LoadBillTask loadBillTask;
	private LinearLayout header;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Bundle extras = getIntent().getExtras();
		id = extras.getString("id");
		code = extras.getString("code");
		title = extras.getString("title");
		introduced_at = extras.getLong("introduced_at");
		
		setupControls();
		
		BillInfoHolder holder = (BillInfoHolder) getLastNonConfigurationInstance();
        if (holder != null) {
        	bill = holder.bill;
        	loadBillTask = holder.loadBillTask;
        }
		
		loadBill();
	}
	
	@Override
	public Object onRetainNonConfigurationInstance() {
		return new BillInfoHolder(bill, loadBillTask);
	}
	
	public void loadBill() {
		if (loadBillTask != null)
			loadBillTask.onScreenLoad(this);
		else {
			if (bill != null)
				displayBill();
			else
				loadBillTask = (LoadBillTask) new LoadBillTask(this).execute(id);
		}
	}
	
	public void onLoadBill(Bill bill) {
		this.bill = bill;
		displayBill();
	}
	
	public void onLoadBill(CongressException exception) {
		Utils.alert(this, exception);
		finish();
	}
	
	public void setupControls() {
		LayoutInflater inflater = LayoutInflater.from(this);
		header = (LinearLayout) inflater.inflate(R.layout.bill_header, null);
		((TextView) header.findViewById(R.id.code)).setText(Bill.formatCode(code));
		TextView titleView = (TextView) header.findViewById(R.id.title);
		
		String displayTitle = (title.length() > 400) ? Utils.truncate(title, 400) : title;
		titleView.setText(displayTitle);
		titleView.setTextSize(sizeOfTitle(displayTitle));
		
		String date = "Introduced on " + new SimpleDateFormat("MMM dd, yyyy").format(new Date(introduced_at));
		((TextView) header.findViewById(R.id.introduced)).setText(date);
		
		((TextView) header.findViewById(R.id.loading_message)).setText("Loading bill details...");
		
		MergeAdapter adapter = new MergeAdapter();
		adapter.addView(header);
		setListAdapter(adapter);
	}
	
	public void displayBill() {
		LayoutInflater inflater = LayoutInflater.from(this);
		Legislator legislator = bill.sponsor;
		
		header.findViewById(R.id.loading).setVisibility(View.GONE);
		
		LinearLayout sponsor = (LinearLayout) inflater.inflate(R.layout.sponsor, null);
		String name = legislator.title + ". " + legislator.getName() + " (" + legislator.party + "-" + legislator.state + ")";
		((TextView) sponsor.findViewById(R.id.name)).setText(name);
		
		sponsor.setTag("sponsor");
		ArrayList<View> sponsorViews = new ArrayList<View>(1);
		sponsorViews.add(sponsor);
		
		LinearLayout summary = (LinearLayout) inflater.inflate(R.layout.bill_summary, null);
		((TextView) summary.findViewById(R.id.header_text)).setText("Summary");
		
		if (bill.summary != null && bill.summary.length() > 0)
			((TextView) summary.findViewById(R.id.summary)).setText(bill.summary);
		else {
			summary.findViewById(R.id.summary).setVisibility(View.GONE);
			summary.findViewById(R.id.no_summary).setVisibility(View.VISIBLE);
		}
		
		MergeAdapter adapter = (MergeAdapter) getListAdapter();
		adapter.addAdapter(new ViewArrayAdapter(this, sponsorViews));
		adapter.addView(summary);
		setListAdapter(adapter);
	}
	
	@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
		String type = (String) v.getTag();
    	if (type.equals("sponsor"))
    		startActivity(Utils.legislatorIntent(bill.sponsor.bioguide_id));
    }
	
	public int sizeOfTitle(String title) {
		int length = title.length();
		if (length <= 100)
			return 18;
		else if (length <= 200)
			return 16;
		else if (length <= 300)
			return 14;
		else if (length <= 400)
			return 12;
		else // should be truncated above this anyhow
			return 12;
	}
	
	private class LoadBillTask extends AsyncTask<String,Void,Bill> {
		private BillInfo context;
		private CongressException exception;
		
		public LoadBillTask(BillInfo context) {
			this.context = context;
		}
		
		public void onScreenLoad(BillInfo context) {
			this.context = context;
		}
		
		@Override
		public Bill doInBackground(String... billId) {
			try {
				Bill bill = Bill.find(billId[0], "basic,extended,sponsor,summary");
				
				return bill;
			} catch (CongressException exception) {
				this.exception = exception;
				return null;
			}
		}
		
		@Override
		public void onPostExecute(Bill bill) {
			context.loadBillTask = null;
			
			if (exception != null && bill == null)
				context.onLoadBill(exception);
			else
				context.onLoadBill(bill);
		}
	}
	
	static class BillInfoHolder {
		LoadBillTask loadBillTask;
		Bill bill;
		
		public BillInfoHolder(Bill bill, LoadBillTask loadBillTask) {
			this.bill = bill;
			this.loadBillTask = loadBillTask;
		}
	}
}