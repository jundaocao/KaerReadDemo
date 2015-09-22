package com.kaer.card.demo;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends ListActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		initListView();
	}

	private void initListView() {
		// TODO Auto-generated method stub
		ArrayList<String> list = new ArrayList<String>();
		list.add("NFC读取身份证");
		list.add("OTG读取身份证");
		ArrayAdapter<String> arr = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, list);
		setListAdapter(arr);

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// TODO Auto-generated method stub
		super.onListItemClick(l, v, position, id);
		switch (position) {
		case 0:
			startActivity(new Intent(MainActivity.this, NFCReadActivity.class));
			break;
		case 1:
			startActivity(new Intent(MainActivity.this, OtgReadActivity.class));

			break;
		default:
			break;
		}
	}

}
