package com.example.denish.bloodbank;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //RDX - @a123456 , DENRAN
    private static final String TAG = "MainActivity";
    public static final int RC_SIGN_IN = 1;
    public static final int RC_TYPE = 123;
    private static final String ANONYMOUS = "anonymous";
//    String type;

    private ListView mListView;
    private DataAdapter mDataAdapter;
    private ProgressBar mProgressBar;
    private Button temp;

    private String mUsername;
    private String mMobileNumber;
    private String mGroup;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mUserDatabaseReference;
    private ChildEventListener mChildEventListener;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate: starts");
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();

        mUserDatabaseReference = mFirebaseDatabase.getReference().child("users");

        // initializing references to views
        //mProgressBar = findViewById(R.id.progress_bar);
        mListView = findViewById(R.id.itemListView);
        temp = findViewById(R.id.temp_button);

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                Log.d(TAG, "onAuthStateChanged: inside auth");
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    onSignedInInitialized(user.getDisplayName());

                } else {
                    // User is signed out
                    onSignedOutCleanup();
                    Log.d(TAG, "onAuthStateChanged: starting auth activity");
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.EmailBuilder().build(),
                                            new AuthUI.IdpConfig.GoogleBuilder().build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };

        Boolean isFirstRun = getSharedPreferences("PREFERENCE", MODE_PRIVATE)
                .getBoolean("isFirstRun", true);

        if (isFirstRun) {
            Log.d(TAG, "onCreate: before Intent of SelectGroup");
            Intent i1 = new Intent(getApplicationContext(),SelectGroup.class);
            startActivity(i1);
            Log.d(TAG, "onCreate: after Intent of SelectGroup");
            Toast.makeText(MainActivity.this, "First Run", Toast.LENGTH_LONG)
                    .show();
        }

        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode

        mGroup = pref.getString("group", null);
        mMobileNumber = pref.getString("mobile", null);
        Log.d(TAG, "onCreate: (Group,Mobile) : " + mGroup + "," + mMobileNumber);
        getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit()
                .putBoolean("isFirstRun", false).apply();


        List<DataItem> dataItems = new ArrayList<>();
        mDataAdapter = new DataAdapter(this,R.layout.list_item,dataItems);
        mListView.setAdapter(mDataAdapter);

        temp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: Temp clicked");

                // Clear input box
                DataItem dataItem =
                        new DataItem(mUsername,mMobileNumber,mGroup);
                mUserDatabaseReference.push().setValue(dataItem);
                Log.d(TAG, "onClick: data sent");
            }
        });
        

    }

    private void onSignedInInitialized(String displayName) {
        Log.d(TAG, "onSignedInInitialized: starts");
        //Log.d(TAG, "onSignedInInitialized: IsFirstRun "+ isFirstRunClone);
        mUsername = displayName;
        //isFirstRunClone = true;
        attachDatabaseReadListener();
    }

    private void onSignedOutCleanup(){
        Log.d(TAG, "onSignedOutCleanup: starts");
        //Log.d(TAG, "onSignedOutCleanup: IsFirstRun "+ isFirstRunClone);
        mUsername = ANONYMOUS;
        mDataAdapter.clear();
        //isFirstRunClone = true;
        detachDatabaseReadListener();
    }



    private void attachDatabaseReadListener(){
        Log.d(TAG, "attachDatabaseReadListener: attached");
        if(mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    DataItem dataItem = dataSnapshot.getValue(DataItem.class);
                    mDataAdapter.add(dataItem);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };

            mUserDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    private void detachDatabaseReadListener(){
        Log.d(TAG, "detachDatabaseReadListener: detached");
        if(mChildEventListener != null) {
            mUserDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu: created");
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: selecting option");
        switch (item.getItemId()){
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                //isFirstRunClone = true;
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume: starts");
        //Log.d(TAG, "onResume: IsFirstRun " + isFirstRunClone);
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: starts");
        super.onPause();
        //Log.d(TAG, "onPause: IsFirstRun "+ isFirstRunClone);
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        detachDatabaseReadListener();
        mDataAdapter.clear();
    }

//    @Override
//    protected void onStop() {
//        super.onStop();
//        isFirstRunClone = true;
//    }
//
//    @Override
//    protected void onRestart() {
//        super.onRestart();
//        isFirstRunClone = true;
//    }

    //    @Override
//    protected void onRestart() {
//        Log.d(TAG, "onRestart: starts");
//        super.onRestart();
//        getSharedPreferences("PREFERENCE", MODE_PRIVATE).edit()
//                .putBoolean("isFirstRun", true).commit();
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: starts");
        super.onActivityResult(requestCode, resultCode, data);

//        if(requestCode == RC_TYPE){
//            mGroup = data.getStringExtra("group");
//            mMobileNumber = data.getStringExtra("mobile");
//            Log.d(TAG, "onActivityResult: Group and Mobile : " + mGroup + " , " + mMobileNumber);
//        }

        if(requestCode == RC_SIGN_IN){
            if(resultCode == RESULT_OK){
                Toast.makeText(this, "Signed In", Toast.LENGTH_SHORT).show();
            }
            else if(resultCode == RESULT_CANCELED){
                Toast.makeText(this, "Signed In Cancelled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
