package org.kvj.whiskey2.widgets;

import java.util.List;

import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.form.BundleAdapter;
import org.kvj.bravo7.form.FormController;
import org.kvj.bravo7.form.WidgetBundleAdapter;
import org.kvj.bravo7.form.impl.bundle.LongBundleAdapter;
import org.kvj.bravo7.form.impl.widget.TextViewStringAdapter;
import org.kvj.bravo7.form.impl.widget.TransientAdapter;
import org.kvj.whiskey2.R;
import org.kvj.whiskey2.Whiskey2App;
import org.kvj.whiskey2.data.DataController;
import org.kvj.whiskey2.data.SheetInfo;
import org.kvj.whiskey2.data.TemplateGroup;
import org.kvj.whiskey2.data.TemplateInfo;
import org.kvj.whiskey2.widgets.SheetDialogFragment.SelectTemplateDialogFragment.TemplateSelectResult;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;

public class SheetDialogFragment extends SherlockDialogFragment {

	public static class SelectTemplateDialogFragment extends SherlockDialogFragment {

		List<TemplateGroup> data = null;
		TemplateSelectResult listener = null;

		public static interface TemplateSelectResult {
			public void onResult(TemplateInfo info);
		}

		public class SelectTemplateAdapter extends BaseExpandableListAdapter {

			@Override
			public TemplateInfo getChild(int groupPosition, int childPosition) {
				return getGroup(groupPosition).templates.get(childPosition);
			}

			@Override
			public long getChildId(int groupPosition, int childPosition) {
				return getChild(groupPosition, childPosition).id;
			}

			@Override
			public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
					ViewGroup parent) {
				LayoutInflater inflater = (LayoutInflater) parent.getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.template_list_item, parent, false);
				TextView textView = (TextView) convertView.findViewById(R.id.template_list_item_title);
				textView.setText(getChild(groupPosition, childPosition).name);
				return convertView;
			}

			@Override
			public int getChildrenCount(int groupPosition) {
				return getGroup(groupPosition).templates.size();
			}

			@Override
			public TemplateGroup getGroup(int groupPosition) {
				return data.get(groupPosition);
			}

			@Override
			public int getGroupCount() {
				return data.size();
			}

			@Override
			public long getGroupId(int groupPosition) {
				return groupPosition;
			}

			@Override
			public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
				LayoutInflater inflater = (LayoutInflater) parent.getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(R.layout.template_list_item, parent, false);
				TextView textView = (TextView) convertView.findViewById(R.id.template_list_item_title);
				textView.setText(getGroup(groupPosition).title);
				return convertView;
			}

			@Override
			public boolean hasStableIds() {
				return true;
			}

			@Override
			public boolean isChildSelectable(int groupPosition, int childPosition) {
				return true;
			}

		}

		public SelectTemplateDialogFragment() {
		}

		public void init(List<TemplateGroup> data, TemplateSelectResult listener) {
			this.data = data;
			adapter = new SelectTemplateAdapter();
			this.listener = listener;
		}

		private SelectTemplateAdapter adapter;

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			if (null == adapter) { // Invalid
				return null;
			}
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle("Select template");
			ExpandableListView listView = new ExpandableListView(getActivity());
			listView.setAdapter(adapter);
			builder.setView(listView);
			listView.expandGroup(adapter.getGroupCount() - 1);
			listView.setOnChildClickListener(new OnChildClickListener() {

				@Override
				public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition,
						long id) {
					TemplateInfo info = adapter.getChild(groupPosition, childPosition);
					dismiss();
					listener.onResult(info);
					return true;
				}
			});
			builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			return builder.create();
		}
	}

	private static final String KEY_ID = "id";
	private static final String KEY_TEMPLATE = "template_id";
	private static final String KEY_TITLE = "title";
	private static final String KEY_NOTEBOOK = "notebook_id";
	protected static final String TAG = "SheetDialog";

	class TemplateButtonAdapter extends WidgetBundleAdapter<Button, Long> {

		private long value = -1L;

		public TemplateButtonAdapter(BundleAdapter<Long> adapter) {
			super(adapter, -1L);
		}

		@Override
		public Long getWidgetValue(Bundle bundle) {
			return value;
		}

		@Override
		public void setWidgetValue(Long value, Bundle bundle) {
			TemplateInfo info = SheetDialogFragment.this.controller.getTemplate(value);
			selectTemplateButton.setText(info.name);
			this.value = value;
		}

	}

	public static SheetDialogFragment newInstance(SheetInfo sheet, long notebookID) {
		SheetDialogFragment instance = new SheetDialogFragment();
		Bundle args = new Bundle();
		args.putLong(KEY_ID, sheet.id);
		args.putLong(KEY_TEMPLATE, sheet.templateID);
		args.putLong(KEY_NOTEBOOK, notebookID);
		args.putString(KEY_TITLE, sheet.title);
		instance.setArguments(args);
		return instance;
	}

	private DataController controller;
	private FormController formController;
	private Button selectTemplateButton;

	public SheetDialogFragment() {
		super();
		controller = Whiskey2App.getInstance().getBean(DataController.class);
	}

	@Override
	public void onSaveInstanceState(Bundle data) {
		super.onSaveInstanceState(data);
		formController.save(data);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle("Sheet editor");
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.sheet_editor, null);
		selectTemplateButton = (Button) view.findViewById(R.id.edit_template);
		formController = new FormController(view);
		formController.add(new TransientAdapter<Long>(new LongBundleAdapter(), -1L), KEY_ID);
		formController.add(new TransientAdapter<Long>(new LongBundleAdapter(), -1L), KEY_NOTEBOOK);
		formController.add(new TextViewStringAdapter(R.id.edit_title, ""), KEY_TITLE);
		formController.add(new TemplateButtonAdapter(new LongBundleAdapter()), KEY_TEMPLATE);
		formController.load(this, savedInstanceState);
		builder.setView(view);
		selectTemplateButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				selectTemplate();
			}
		});
		Button saveButton = (Button) view.findViewById(R.id.editor_save);
		Button removeButton = (Button) view.findViewById(R.id.editor_remove);
		Button cancelButton = (Button) view.findViewById(R.id.editor_cancel);
		if (-1 == formController.getValue(KEY_ID, Long.class)) {
			removeButton.setVisibility(View.GONE);
		} else {
			removeButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					removeClick();
				}
			});
		}
		saveButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				saveClick();
			}
		});
		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onCancelClick();
			}
		});
		setCancelable(false);
		return builder.create();
	}

	protected void saveClick() {
		String name = formController.getValue(KEY_TITLE, String.class).trim();
		if (TextUtils.isEmpty(name)) { // Invalid value
			SuperActivity.notifyUser(getActivity(), "Name is required");
			return;
		}
		long id = formController.getValue(KEY_ID, Long.class);
		boolean result = true;
		long template = formController.getValue(KEY_TEMPLATE, Long.class);
		long notebook = formController.getValue(KEY_NOTEBOOK, Long.class);
		if (-1 == id) { // New sheet
			SheetInfo info = controller.newSheet(name, template, notebook);
			result = info != null;
		} else {
			result = controller.updateSheet(id, name, template);
		}
		if (!result) { // Error
			SuperActivity.notifyUser(getActivity(), "Error saving sheet");
			return;
		} else {
			dismiss();
			controller.notifyDataChanged();
		}
	}

	protected void removeClick() {
		final long id = formController.getValue(KEY_ID, Long.class);
		SuperActivity.showQuestionDialog(getActivity(), "Remove sheet?",
				"Are you sure want to remove sheet? It'll also remove notes", new Runnable() {

					@Override
					public void run() {
						boolean result = controller.removeSheet(id);
						if (!result) { // Error
							SuperActivity.notifyUser(getActivity(), "Error removing sheet");
							return;
						} else {
							dismiss();
							controller.notifyDataChanged();
						}
					}
				});
	}

	protected void selectTemplate() {
		SelectTemplateDialogFragment fragment = new SelectTemplateDialogFragment();
		fragment.init(controller.getTemplates(), new TemplateSelectResult() {

			@Override
			public void onResult(TemplateInfo info) {
				formController.getAdapter(KEY_TEMPLATE, TemplateButtonAdapter.class).setWidgetValue(info.id);
			}
		});
		fragment.show(getSherlockActivity().getSupportFragmentManager(), "templates");
	}

	private void onCancelClick() {
		if (formController.changed()) {
			SuperActivity.showQuestionDialog(getActivity(), "Dismiss changes?",
					"There are unsaved changes. Are you sure want to continue?", new Runnable() {

						@Override
						public void run() {
							dismiss();
						}
					});
		} else {
			dismiss();
		}
	}
}
