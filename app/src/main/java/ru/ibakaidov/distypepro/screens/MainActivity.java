package ru.ibakaidov.distypepro.screens;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.FirebaseApp;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Arrays;
import java.util.List;

import ru.ibakaidov.distypepro.DatabaseManager;
import ru.ibakaidov.distypepro.R;
import ru.ibakaidov.distypepro.components.FileSynthesizer;
import ru.ibakaidov.distypepro.components.SayBox;
import ru.ibakaidov.distypepro.components.StatementsList;
import ru.ibakaidov.distypepro.controllers.CategoriesController;
import ru.ibakaidov.distypepro.controllers.StatementsController;
import ru.ibakaidov.distypepro.utils.CookiesManager;
import ru.ibakaidov.distypepro.utils.ImportManager;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1001;
    private static final int CHATS_COUNT = 3;
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    static {

        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FirebaseApp.initializeApp(MainActivity.this);


        FirebaseAuth.getInstance().addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(firebaseAuth.getCurrentUser()==null){
                    signin();
                } else {

                    CookiesManager cookiesManager = new CookiesManager(getApplicationContext());

                    if(!cookiesManager.getDataImported()){
                        ImportManager.importData(new DatabaseManager(MainActivity.this), new CategoriesController(getApplicationContext()), new StatementsController(getApplicationContext(), null));
                        cookiesManager.setDataImported(true);
                    }

                    // Create the adapter that will return a fragment for each of the three
                    // primary sections of the activity.
                    mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

                    // Set up the ViewPager with the sections adapter.
                    mViewPager = (ViewPager) findViewById(R.id.container);
                    mViewPager.setAdapter(mSectionsPagerAdapter);

                    TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);

                    mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
                    tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));

                }
            }
        });
        signin();

    }

    private void signin() {

        FirebaseAuth mAuth = FirebaseAuth.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {

            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build(),
                    new AuthUI.IdpConfig.PhoneBuilder().build(),
                    new AuthUI.IdpConfig.GoogleBuilder().build()
            );

// Create and launch sign-in intent
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    RC_SIGN_IN);
            return;
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();
        PlaceholderFragment placeholder = getCurrentSpeechFragment();
        SayBox sayBox = placeholder.getSayBox();
        StatementsList statementsList = placeholder.getStatementsList();
        //noinspection SimplifiableIfStatement
        if(id==R.id.action_clear){
            sayBox.clear();
            return true;
        }

        if (id == R.id.action_show) {
            SpotlightActivity.show(this, sayBox.getText());
            return true;
        }
        if(id == R.id.action_save){
                        statementsList.saveCurrentStatement();
            return  true;
        }
//        if(id==R.id.action_synth_to_file){
//            FileSynthesizer.show(this);
//        }
        if(id==R.id.action_tts_settings){
            Intent intent = new Intent();
            intent.setAction("com.android.settings.TTS_SETTINGS");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(intent);
            return true;
        }
        if(id==R.id.action_signout){
            FirebaseAuth.getInstance().signOut();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.getInstance(position);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return CHATS_COUNT;
        }
    }

    public PlaceholderFragment getCurrentSpeechFragment() {
        int index = mViewPager.getCurrentItem();
        return (PlaceholderFragment) ((SectionsPagerAdapter) mViewPager.getAdapter()).getItem(index);
    }
}
