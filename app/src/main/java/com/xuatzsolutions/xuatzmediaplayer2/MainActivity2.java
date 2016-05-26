package com.xuatzsolutions.xuatzmediaplayer2;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by xuatz on 27/9/2015.
 */
public class MainActivity2 extends Activity {
    private String[] activities;

    RecyclerView mRecyclerView;                           // Declaring RecyclerView
    RecyclerView.Adapter mAdapter;                        // Declaring Adapter For Recycler View
    RecyclerView.LayoutManager mLayoutManager;            // Declaring Layout Manager as a linear layout manager
    DrawerLayout Drawer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view); // Assigning the RecyclerView Object to the xml View
        mRecyclerView.setHasFixedSize(true);                            // Letting the system know that the list objects are of fixed size

        activities = getResources().getStringArray(R.array.activities_name);
        mAdapter = new MySideNavViewAdapter(activities);       // Creating the Adapter of MyAdapter class(which we are going to see in a bit)

        mRecyclerView.setAdapter(mAdapter);                              // Setting the adapter to RecyclerView
        mRecyclerView.setLayoutManager(mLayoutManager);

        mRecyclerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayView(position);
            }
        });

        mDrawerList.setOnItemClickListener(new SlideMenuClickListener());

        /**
         * Slide menu item click listener
         * */
        private class SlideMenuClickListener implements
                ListView.OnItemClickListener {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                // display view for selected nav drawer item

            }
        }

        private void displayView(int position) {
            // update the main content by replacing fragments
            Fragment fragment = null;
            switch (position) {
                case 0:
                    fragment = new HomeFragment();
                    break;
                case 1:
                    fragment = new FindPeopleFragment();
                    break;
                case 2:
                    fragment = new PhotosFragment();
                    break;
                case 3:
                    fragment = new CommunityFragment();
                    break;
                case 4:
                    fragment = new PagesFragment();
                    break;
                case 5:
                    fragment = new WhatsHotFragment();
                    break;

                default:
                    break;
            }

            if (fragment != null) {
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.frame_container, fragment).commit();

                // update selected item and title, then close the drawer
                mDrawerList.setItemChecked(position, true);
                mDrawerList.setSelection(position);
                setTitle(navMenuTitles[position]);
                mDrawerLayout.closeDrawer(mDrawerList);
            } else {
                // error in creating fragment
                Log.e("MainActivity", "Error in creating fragment");
            }
        }
    }
}
