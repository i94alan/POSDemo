package com.braulio.chairman;

import java.text.NumberFormat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


//MainActivity is not used; instead, DeviceScanActivity is main launcher
public class MainActivity extends Activity {

    int numberOfCoffees;

    double totalPrice = 0;
    boolean hasChocolate;
    boolean hasWhippedCream;
    private final int coffee_price = 3; // 3 dollars

    private void setActivityLogoTitle(){
        android.app.ActionBar actionbar = getActionBar(); //this will return null if in androidmanifest, application uses         android:theme="@style/AppTheme">
        actionbar.setDisplayShowHomeEnabled(true);
        actionbar.setLogo(R.drawable.coffee_icon);
        actionbar.setTitle("Coffee order menu");
        actionbar.setDisplayUseLogoEnabled(true);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setActivityLogoTitle();
    }

    /**
     * This method is called when the + button is clicked.
     */
    public void incrementCoffeeMain(View view) {
        if (numberOfCoffees <= 49) {
            numberOfCoffees = numberOfCoffees + 1;
            displayCoffeeQuantity(numberOfCoffees);
            displayTotalPrice(numberOfCoffees,hasWhippedCream,hasChocolate);
        }
        else {
            Toast.makeText(this, getString(R.string.fifty_cups), Toast.LENGTH_SHORT).show();
        }

    }
    /**
     * This method is called when the - button is clicked.
     */
    public void decrementCoffeeMain(View view) {
        if (numberOfCoffees >= 1) {
            numberOfCoffees = numberOfCoffees - 1;
            displayCoffeeQuantity(numberOfCoffees);
            displayTotalPrice(numberOfCoffees,hasWhippedCream,hasChocolate);
        }
        else {
            Toast.makeText(this, getString(R.string.negative_cups), Toast.LENGTH_SHORT).show();
        }
    }

//    public void addWhippedCream(View view) {
//        CheckBox whippedCreamCheckBox = (CheckBox) findViewById(R.id.whipped_cream_checkbox);
//        hasWhippedCream = whippedCreamCheckBox.isChecked();
//        displayTotalPrice(numberOfCoffees,hasWhippedCream,hasChocolate);
//    }
//
//    public void addChocolate(View view) {
//        CheckBox chocolateCheckBox = (CheckBox) findViewById(R.id.chocolate_checkbox);
//        hasChocolate = chocolateCheckBox.isChecked();
//        displayTotalPrice(numberOfCoffees,hasWhippedCream,hasChocolate);
//    }



    /**
     * This method displays the given quantity value on the screen.
     */
    public void displayCoffeeQuantity(int number) {
        TextView quantityTextView = (TextView) findViewById(R.id.quantity_text_view);
        quantityTextView.setText("" + number);
    }
    /**
     * This method displays the given totalPrice on the screen.
     */
    public void displayTotalPrice(int quantity, boolean hasWhippedCream, boolean hasChocolate) {
        double priceChocolate = 0.5;
        double priceWhippedCream = 0.5;
        totalPrice = quantity*coffee_price; //3 dollars
        if (hasWhippedCream) {
            totalPrice += priceWhippedCream * quantity;
        }
        if (hasChocolate) {
            totalPrice += priceChocolate*quantity;
        }
        TextView priceTextView = (TextView) findViewById(R.id.price_text_view);
        priceTextView.setText(getString(R.string.price) +": " + NumberFormat.getCurrencyInstance().format(totalPrice));
    }



    public void payOrder(View view){

//        if (mBluetoothLeService != null) {
//            final byte[] cmd_DI3={0x44,0x49,0x33};
//            mBluetoothLeService.sendData(Utility.getCommand(cmd_DI3));
//        }
        Toast.makeText(this, "submit order", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.ble_device_scan_menu:
                final Intent intent = new Intent(this, DeviceScanActivity.class);
                startActivity(intent);
                Toast.makeText(this, "You have selected BLE scan", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


}
