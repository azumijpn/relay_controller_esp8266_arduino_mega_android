package bhsystems.eu.relaycontroller.buttons;

import android.content.Context;
import android.content.Intent;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bhsystems.eu.relaycontroller.R;
import bhsystems.eu.relaycontroller.application.RelayControllerApplication;
import bhsystems.eu.relaycontroller.entity.RelayControllerButton;

public class MainActivity extends AppCompatActivity implements ButtonsAdapter.ButtonSelectedListener {
    private static final int BUTTON_REQUEST_CODE = 1212;
    private static final String BUTTONS = "buttons";
    // Network Service Discovery related members
// This allows the app to discover the garagedoor.local
// "service" on the local network.
// Reference: http://developer.android.com/training/connect-devices-wirelessly/nsd.html
    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.ResolveListener mResolveListener;
    private NsdServiceInfo mServiceInfo;
    public String mRPiAddress;
    // The NSD service type that the RPi exposes.
    private static final String SERVICE_TYPE = "_http._tcp.";


    private RecyclerView rvButtons;
    private ButtonsAdapter buttonsAdapter;

    private ArrayList<RelayControllerButton> buttons = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.app_name);

        rvButtons = findViewById(R.id.rv_buttons);

        if (savedInstanceState != null) {
            buttons = savedInstanceState.getParcelableArrayList(BUTTONS);
        } else {
            reloadButtons();
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                startActivityForResult(new Intent(MainActivity.this, NewButtonActivity.class), BUTTON_REQUEST_CODE);
            }
        });
        mRPiAddress = "";
        mNsdManager = (NsdManager) (getApplicationContext().getSystemService(Context.NSD_SERVICE));
        initializeResolveListener();
        initializeDiscoveryListener();
        mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(BUTTONS, buttons);
    }

    private void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                String name = service.getServiceName();
                String type = service.getServiceType();
                Log.d("NSD", "Service Name=" + name);
                Log.d("NSD", "Service Type=" + type);
                if (type.equals(SERVICE_TYPE) && name.contains("relaycontroller")) {
                    Log.d("NSD", "Service Found @ '" + name + "'");
                    mNsdManager.resolveService(service, mResolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

    private void initializeResolveListener() {
        mResolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.e("NSD", "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                mServiceInfo = serviceInfo;
                InetAddress host = mServiceInfo.getHost();
                String address = host.getHostAddress();
                Log.d("NSD", "Resolved address = " + address);
                Toast.makeText(getApplicationContext(), address, Toast.LENGTH_LONG).show();
                changeButtonsState(true);
                mRPiAddress = address;
            }
        };
    }

    private void changeButtonsState(final boolean enabled) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(buttonsAdapter != null)
                buttonsAdapter.notifyDataSetChanged();
                for (RelayControllerButton button : buttons) {
                    button.setEnabled(enabled);
                }
            }
        });
    }


    private void prepareRecycleView() {
        rvButtons.setLayoutManager(new LinearLayoutManager(this));
        buttonsAdapter = new ButtonsAdapter(this);
        buttonsAdapter.addAll(buttons);
        rvButtons.setAdapter(buttonsAdapter);
    }

    @Override
    public void onButtonClicked(RelayControllerButton relayControllerButton) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://" + mRPiAddress + "/" + relayControllerButton.getPin();

// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        Log.d("NSD", response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("NSD", error.getMessage());
            }
        });
// Add the request to the RequestQueue.
        queue.add(stringRequest);
        Toast.makeText(MainActivity.this, "Button clicked: " + relayControllerButton.getLabel(), Toast.LENGTH_SHORT).show();
    }

    static class ButtonsLoadAsyncTask extends AsyncTask<Void, Void, List<RelayControllerButton>> {
        @Override
        protected List<RelayControllerButton> doInBackground(Void... voids) {
            return RelayControllerApplication.getInstance().getDb().relayControllerButtonDao().getAll();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BUTTON_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    reloadButtons();
                    mRPiAddress = "";
                    mNsdManager = (NsdManager) (getApplicationContext().getSystemService(Context.NSD_SERVICE));
                    initializeResolveListener();
                    initializeDiscoveryListener();
                    mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void reloadButtons() {
        ButtonsLoadAsyncTask buttonsLoadAsyncTask = new ButtonsLoadAsyncTask() {
            @Override
            protected void onPostExecute(List<RelayControllerButton> relayControllerButtons) {
                super.onPostExecute(relayControllerButtons);
                buttons = new ArrayList<>(relayControllerButtons);
                prepareRecycleView();
                changeButtonsState(false);
            }
        };
        buttonsLoadAsyncTask.execute();
    }
}