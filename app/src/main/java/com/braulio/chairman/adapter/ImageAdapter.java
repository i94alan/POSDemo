package com.braulio.chairman.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.braulio.chairman.DeviceScanActivity;
import com.braulio.chairman.R;

public class ImageAdapter extends BaseAdapter {
	private Context context;
	private final String[] menuItemValues;

	private boolean isPosition0 = false;

	public ImageAdapter(Context context, String[] menuItemValues) {
		this.context = context;
		this.menuItemValues = menuItemValues;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View gridView;

		if (convertView == null) {

			gridView = new View(context);

			// get layout from menu_item_place_holder.xml
			gridView = inflater.inflate(R.layout.menu_item_place_holder, null); //2nd param, Viewgroup root , ViewGroup: Optional view to be the parent of the generated hierarchy. This value may be null.

			// set value into textview
			String menuItem = menuItemValues[position];
			TextView textView = (TextView) gridView.findViewById(R.id.menuItemName);
			textView.setText(menuItem);

			// set item image, decrement and increment methods
			ImageView imageView = (ImageView) gridView.findViewById(R.id.menuItemImage);
			Button bt_decrement, bt_increment;
			final TextView tv_quantity = (TextView) gridView.findViewById(R.id.tv_quantity);


			if (menuItem.equals("coffee")) {
				//seems like getView() with position 0 is being called twice , 0 1 2 3 0, and this will ignore 2nd one, need to google why it's called twice
				if(!isPosition0) {
					isPosition0 = true;
					((DeviceScanActivity) context).getCoffee().setTextViewQuantity(tv_quantity);
				}

				imageView.setImageResource(R.drawable.coffee);
				bt_decrement = (Button) gridView.findViewById(R.id.bt_decrement);
				bt_decrement.setOnClickListener(new View.OnClickListener() {
					@Override //can either no @Override, or use @Override, but when use @Override, it needs to really override correct method name
					public void onClick(View v) {
						((DeviceScanActivity) context).getCoffee().decrementCoffee(tv_quantity);
					}
				});
				bt_increment = (Button) gridView.findViewById(R.id.bt_increment);
				bt_increment.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						((DeviceScanActivity) context).getCoffee().incrementCoffee(tv_quantity);
					}
				});


			} else if (menuItem.equals("donut")) {
				((DeviceScanActivity) context).getDonut().setTextViewQuantity(tv_quantity);
				imageView.setImageResource(R.drawable.donut);
				bt_decrement = (Button) gridView.findViewById(R.id.bt_decrement);
				bt_decrement.setOnClickListener(new View.OnClickListener() {
					@Override //can either no @Override, or use @Override, but when use @Override, it needs to really override correct method name
					public void onClick(View v) {
						((DeviceScanActivity) context).getDonut().decrementDonut(tv_quantity);
					}
				});
				bt_increment = (Button) gridView.findViewById(R.id.bt_increment);
				bt_increment.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						((DeviceScanActivity) context).getDonut().incrementDonut(tv_quantity);
					}
				});


			} else if (menuItem.equals("tea")) {
				((DeviceScanActivity) context).getTea().setTextViewQuantity(tv_quantity);
				imageView.setImageResource(R.drawable.tea);
				bt_decrement = (Button) gridView.findViewById(R.id.bt_decrement);
				bt_decrement.setOnClickListener(new View.OnClickListener() {
					@Override //can either no @Override, or use @Override, but when use @Override, it needs to really override correct method name
					public void onClick(View v) {
						((DeviceScanActivity) context).getTea().decrementTea(tv_quantity);
					}
				});
				bt_increment = (Button) gridView.findViewById(R.id.bt_increment);
				bt_increment.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						((DeviceScanActivity) context).getTea().incrementTea(tv_quantity);
					}
				});

			} else if(menuItem.equals("croissant")){
				((DeviceScanActivity) context).getCroissant().setTextViewQuantity(tv_quantity);
				imageView.setImageResource(R.drawable.croissant);
				bt_decrement = (Button) gridView.findViewById(R.id.bt_decrement);
				bt_decrement.setOnClickListener(new View.OnClickListener() {
					@Override //can either no @Override, or use @Override, but when use @Override, it needs to really override correct method name
					public void onClick(View v) {
						((DeviceScanActivity) context).getCroissant().decrementCroissant(tv_quantity);
					}
				});
				bt_increment = (Button) gridView.findViewById(R.id.bt_increment);
				bt_increment.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						((DeviceScanActivity) context).getCroissant().incrementCroissant(tv_quantity);
					}
				});
			}

		} else {
			gridView = (View) convertView;
		}

		return gridView;
	}

	@Override
	public int getCount() {
		return menuItemValues.length;
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int i) {
		return i;
	}

}
