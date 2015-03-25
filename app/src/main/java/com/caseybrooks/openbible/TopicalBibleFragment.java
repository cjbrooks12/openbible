package com.caseybrooks.openbible;

import android.content.Context;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.view.ActionMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
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

import com.caseybrooks.androidbibletools.basic.Passage;
import com.caseybrooks.androidbibletools.defaults.DefaultMetaData;
import com.caseybrooks.androidbibletools.search.OpenBibleInfo;
import com.nirhart.parallaxscroll.views.ParallaxListView;

import java.io.IOException;
import java.util.ArrayList;

public class TopicalBibleFragment extends Fragment {
	Context context;

	AutoCompleteTextView searchEditText;
	ArrayAdapter<String> suggestionsAdapter;

	ParallaxListView listView;
	OpenBibleAdapter adapter;

	ActionMode mActionMode;
	ProgressBar progress;
	ImageButton searchButton;

	public static Fragment newInstance() {
		Fragment fragment = new TopicalBibleFragment();
		Bundle args = new Bundle();
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		if(mActionMode != null) mActionMode.finish();
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
		adapter = new OpenBibleAdapter(context, new ArrayList<Passage>(), listView);
		adapter.setOnItemOverflowClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			}
		});
		listView.setAdapter(adapter);

		return view;
	}

//Adapter for the listview in this fragment
//------------------------------------------------------------------------------
	public class OpenBibleAdapter extends BaseAdapter {
		Context context;
		ListView lv;
		ArrayList<Passage> items;

		AdapterView.OnItemClickListener cardClick;
		AdapterView.OnItemClickListener overflowClick;
		AdapterView.OnItemClickListener iconClick;

		public OpenBibleAdapter(Context context, ArrayList<Passage> items, ListView lv) {
			this.context = context;
			this.items = items;
			this.lv = lv;
		}

		public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
			cardClick = listener;
		}

		public void setOnItemMultiselectListener(AdapterView.OnItemClickListener listener) {
			iconClick = listener;
		}

		public void setOnItemOverflowClickListener(AdapterView.OnItemClickListener listener) {
			overflowClick = listener;
		}

		@Override
		public int getCount() {
			return items.size();
		}

		public ArrayList<Passage> getItems() {
			return items;
		}

		public int getSelectedCount() {
			int count = 0;

			for (Passage passage : items) {
				if (passage.getMetadata().getBoolean(DefaultMetaData.IS_CHECKED)) count++;
			}

			return count;
		}

		public ArrayList<Passage> getSelectedItems() {
			ArrayList<Passage> selectedItems = new ArrayList<Passage>();

			for (int i = 0; i < items.size(); i++) {
				Passage passage = items.get(i);
				passage.getMetadata().putInt("LIST_POSITION", i + 1);
				if (passage.getMetadata().getBoolean(DefaultMetaData.IS_CHECKED)) {
					selectedItems.add(passage);
				}
			}

			return selectedItems;
		}

		@Override
		public Passage getItem(int position) {
			Passage passage = items.get(position-1);
			passage.getMetadata().putInt("LIST_POSITION", position);
			return passage;
		}

		//items do not yet exist in the database, so do not have ids
		@Override
		public long getItemId(int position) {
			return 0;
		}

		public void removeItem(Passage item) {
			if (items.contains(item)) {
				items.remove(item);
			}
		}

		public void add(Passage item) {
			this.items.add(item);
		}

		public void add(Passage item, int index) {
			this.items.add(index, item);
		}

		public void addAll(ArrayList<Passage> items) {
			this.items = items;
			for(int i = 0; i < this.items.size(); i++) {
				this.items.get(i).getMetadata().putInt("LIST_POSITION", i + 1);
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

			vh.view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ViewHolder vh = (ViewHolder) v.getTag();
					if (iconClick != null)
						cardClick.onItemClick(lv, vh.view, vh.getPosition(), vh.getId());
				}
			});
			vh.overflow.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ViewHolder vh = (ViewHolder) v.getTag();
					if (iconClick != null)
						overflowClick.onItemClick(lv, vh.overflow, vh.getPosition(), vh.getId());
				}
			});

			view.setTag(vh);
			vh.iconBackground.setTag(vh);
			vh.view.setTag(vh);
			vh.overflow.setTag(vh);

			return view;
		}
	}

	public static class ViewHolder {
		final Context context;
		final Drawable circle;

		public Passage passage;

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
			return passage.getMetadata().getInt("LIST_POSITION");
		}

		private int getId() {
			return passage.getMetadata().getInt(DefaultMetaData.ID);
		}

		private void initialize(Passage passage) {
			this.passage = passage;

			reference.setText(passage.getReference().toString());
			verseText.setText(passage.getText());
			version.setText("ESV");//passage.getVersion().getCode().toUpperCase());
			upcount.setText(passage.getMetadata().getInt("UPVOTES") + " helpful votes");

			iconText.setText(passage.getMetadata().getInt("UPVOTES") + "");

			if(passage.getMetadata().getBoolean(DefaultMetaData.IS_CHECKED)) {
				circle.setColorFilter(new PorterDuffColorFilter(context.getResources().getColor(R.color.open_bible_green), PorterDuff.Mode.SRC_IN));
				iconBackground.setImageDrawable(circle);

				iconText.setVisibility(View.INVISIBLE);
			}
			else {
				circle.setColorFilter(new PorterDuffColorFilter(context.getResources().getColor(R.color.open_bible_brown), PorterDuff.Mode.SRC_IN));
				iconBackground.setImageDrawable(circle);

				iconText.setVisibility(View.VISIBLE);
			}
		}
	}

//asynchronously perform tasks to get suggestions and verses from search topic
//------------------------------------------------------------------------------

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
			int threshold = searchEditText.getThreshold();
			searchEditText.setThreshold(1);
			searchEditText.showDropDown();
			searchEditText.setThreshold(threshold);
			suggestionsAdapter.notifyDataSetChanged();
			progress.setVisibility(View.GONE);
		}

		@Override
		protected Void doInBackground(Character... params) {
			try {
				if(Util.isConnected(context)) {
					for(Character character : params) {
						ArrayList<String> suggestions = OpenBibleInfo.getSuggestions(character);

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

	private class SearchVerseAsync extends AsyncTask<String, Passage, ArrayList<Passage>> {
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
		protected ArrayList<Passage> doInBackground(String... params) {
			try {
				if(Util.isConnected(context)) {
					ArrayList<Passage> passages = new ArrayList<>();
					for(String string : params) {
						passages.addAll(OpenBibleInfo.getVersesFromTopic(string));
					}
					return passages;
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
		protected void onPostExecute(ArrayList<Passage> aVoid) {
			super.onPostExecute(aVoid);

			progress.setVisibility(View.GONE);

			adapter.addAll(aVoid);
		}
	}
}


