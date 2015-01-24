package com.caseybrooks.openbible;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nirhart.parallaxscroll.views.ParallaxListView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;

public class TopicalBibleFragment extends Fragment {
    Context context;

    AutoCompleteTextView searchEditText;
    ArrayAdapter<String> suggestionsAdapter;

    ParallaxListView listView;
    OpenBibleAdapter adapter;

    ProgressBar progress;
    ImageButton searchButton;

    private class PassageData {
        public String reference;
        public String version;
        public int upvotes;
        public String verseText;
        public int position;
    }

    public static Fragment newInstance() {
        Fragment fragment = new TopicalBibleFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();

        ActionBar ab = ((ActionBarActivity) context).getSupportActionBar();
        ab.setTitle("Topical Bible");

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_help:
                final View view = LayoutInflater.from(context).inflate(R.layout.popup_help, null);
                final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setView(view);
                final AlertDialog dialog = builder.create();
                dialog.show();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the main layout for this fragment
        View view = inflater.inflate(R.layout.fragment_topical_bible, container, false);

        this.context = getActivity();

        ColorFilter filter = new PorterDuffColorFilter(getResources().getColor(R.color.forest_green), PorterDuff.Mode.SRC_IN);
        progress = (ProgressBar) view.findViewById(R.id.progress);
        progress.getProgressDrawable().setColorFilter(filter);
        progress.getIndeterminateDrawable().setColorFilter(filter);

        listView = (ParallaxListView) view.findViewById(R.id.parallax_listview);

        //setup header view for parallax list view
        RelativeLayout header = (RelativeLayout) inflater.inflate(R.layout.parallax_open_bible_header, null);
        listView.addParallaxedHeaderView(header);

        searchEditText = (AutoCompleteTextView) header.findViewById(R.id.discoverEditText);
        searchButton = (ImageButton) header.findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = searchEditText.getText().toString();
                if (text.length() > 1) {
                    new SearchVerseAsync().execute(text);
                }
            }
        });
        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String text = searchEditText.getText().toString();
                    if (text.length() > 1) {
                        new SearchVerseAsync().execute(text);
                        return true;
                    }
                }
                return false;
            }
        });
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() == 0) {
                    searchButton.setVisibility(View.GONE);
                }
                else {
                    searchButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        suggestionsAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1);
        searchEditText.setAdapter(suggestionsAdapter);
        searchEditText.addTextChangedListener(new TextWatcher() {
            Character searchedChar;

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i2, int i3) {
                if(searchedChar == null || (s.length() > 0 && s.charAt(0) != searchedChar)) {
                    new GetSuggestionsAsync().execute(s.charAt(0));
                    searchedChar = s.charAt(0);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        searchEditText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                String s = suggestionsAdapter.getItem(position);
                new SearchVerseAsync().execute(s);
            }
        });

        //setup adapter for parallax listview
        adapter = new OpenBibleAdapter(context, new ArrayList<PassageData>(), listView);

        listView.setAdapter(adapter);

        return view;
    }

//Adapter for the listview in this fragment
//------------------------------------------------------------------------------
    public class OpenBibleAdapter extends BaseAdapter {
        Context context;
        ListView lv;
        ArrayList<PassageData> items;

        AdapterView.OnItemClickListener cardClick;
        AdapterView.OnItemClickListener overflowClick;
        AdapterView.OnItemClickListener iconClick;

        public OpenBibleAdapter(Context context, ArrayList<PassageData> items, ListView lv) {
            this.context = context;
            this.items = items;
            this.lv = lv;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        public ArrayList<PassageData> getItems() {
            return items;
        }

        @Override
        public PassageData getItem(int position) {
            PassageData passage = items.get(position-1);
            passage.position =  position;
            return passage;
        }

        //items do not yet exist in the database, so do not have ids
        @Override
        public long getItemId(int position) {
            return 0;
        }

        public void addAll(ArrayList<PassageData> items) {
            this.items = items;
            for(int i = 0; i < this.items.size(); i++) {
                this.items.get(i).position = i + 1;
            }

            notifyDataSetChanged();
        }

        public void clear() {
            items.clear();
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh;
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.list_open_bible, parent, false);
                vh = new ViewHolder(context, view);
                view.setTag(vh);
            } else {
                vh = (ViewHolder) view.getTag();
            }

            vh.initialize(items.get(position));

            view.setTag(vh);

            return view;
        }
    }

    public static class ViewHolder {
        final Context context;
        final Drawable circle;

        public PassageData passage;

        View view;
        TextView reference;
        TextView verseText;
        TextView version;
        TextView upcount;

        ImageView iconBackground;
        TextView iconText;

        ImageView overflow;

        ViewHolder(final Context context, View inflater) {
            this.context = context;
            circle = context.getResources().getDrawable(R.drawable.circle);
            view = inflater.findViewById(R.id.list_open_bible_view);

            reference = (TextView) inflater.findViewById(R.id.item_reference);
            verseText = (TextView) inflater.findViewById(R.id.item_verse);
            version = (TextView) inflater.findViewById(R.id.item_version);
            upcount = (TextView) inflater.findViewById(R.id.item_upcount);

            iconBackground = (ImageView) inflater.findViewById(R.id.icon_background);
            iconText = (TextView) inflater.findViewById(R.id.icon_text);

            overflow = (ImageView) inflater.findViewById(R.id.overflow);
        }

        private int getPosition() {
            return passage.position;
        }

        private int getId() {
            return 0;
        }

        private void initialize(PassageData passage) {
            this.passage = passage;

            reference.setText(passage.reference);
            verseText.setText(passage.verseText);
            version.setText(passage.version);
            upcount.setText(passage.upvotes + " helpful votes");

            iconText.setText(passage.upvotes + "");

            circle.setColorFilter(new PorterDuffColorFilter(context.getResources().getColor(R.color.open_bible_brown), PorterDuff.Mode.SRC_IN));
            iconBackground.setImageDrawable(circle);

            iconText.setVisibility(View.VISIBLE);
        }
    }

//asynchronously perform tasks to get suggestions and verses from search topic
//------------------------------------------------------------------------------
    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return (activeNetwork != null) && activeNetwork.isConnected();
    }

    private class GetSuggestionsAsync extends AsyncTask<Character, Void, Void> {
        String message;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progress.setVisibility(View.VISIBLE);
            progress.setIndeterminate(true);
            progress.setProgress(0);
            suggestionsAdapter.clear();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progress.setVisibility(View.GONE);
            int threshold = searchEditText.getThreshold();
            searchEditText.setThreshold(1);
            searchEditText.showDropDown();
            searchEditText.setThreshold(threshold);
            suggestionsAdapter.notifyDataSetChanged();
        }

        @Override
        protected Void doInBackground(Character... params) {
            try {
                if(isConnected(context)) {
                    for(Character character : params) {
                        ArrayList<String> suggestions = getSuggestions(character);

                        for(String string : suggestions) {
                            suggestionsAdapter.add(string);
                        }
                    }
                    message = "Finished";
                }
                else {
                    message = "Cannot search, no internet connection";
                }
            }
            catch(IOException e2) {
                message = "Error while retrieving verse";
            }

            return null;
        }
    }

    private class SearchVerseAsync extends AsyncTask<String, PassageData, ArrayList<PassageData>> {
        String message;
        int count;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            count = 0;

            progress.setVisibility(View.VISIBLE);
            progress.setIndeterminate(true);
            progress.setProgress(0);
        }

        @Override
        protected ArrayList<PassageData> doInBackground(String... params) {
            try {
                if(isConnected(context)) {
                    return getVersesFromTopic(params[0]);
                }
                else {
                    message = "Cannot search, no internet connection";
                }
            }
            catch(IOException e2) {
                message = "Error while retrieving verse";
            }

            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<PassageData> aVoid) {
            super.onPostExecute(aVoid);

            progress.setVisibility(View.GONE);

            adapter.addAll(aVoid);
        }
    }





    public ArrayList<PassageData> getVersesFromTopic(String topic) throws IOException {
        ArrayList<PassageData> verses = new ArrayList<PassageData>();

        String query = "http://www.openbible.info/topics/" + topic.trim().replaceAll(" ", "_");

        Document doc = Jsoup.connect(query).get();
        Elements passages = doc.select(".verse");

        for(Element element : passages) {
            PassageData passage = new PassageData();
            passage.reference = element.select(".bibleref").first().ownText();
            passage.version = "ESV";
            passage.verseText = element.select("p").get(1).text();

            String notesString = element.select(".note").get(0).ownText();
            passage.upvotes = Integer.parseInt(notesString.replaceAll("\\D", ""));

            verses.add(passage);
        }

        return verses;
    }

    public  ArrayList<String> getSuggestions(char letter) throws IOException {
        ArrayList<String> verses = new ArrayList<String>();

        String query = "http://www.openbible.info/topics/" + letter;

        Document doc = Jsoup.connect(query).get();
        Elements passages = doc.select("li");

        for (Element element : passages) {
            verses.add(element.text());
        }

        return verses;
    }
}

