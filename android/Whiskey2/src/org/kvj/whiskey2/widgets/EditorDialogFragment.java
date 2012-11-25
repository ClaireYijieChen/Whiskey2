package org.kvj.whiskey2.widgets;

import org.kvj.bravo7.SuperActivity;
import org.kvj.bravo7.form.FormController;
import org.kvj.bravo7.form.ViewBundleAdapter;
import org.kvj.bravo7.form.impl.bundle.IntegerBundleAdapter;
import org.kvj.bravo7.form.impl.bundle.LongBundleAdapter;
import org.kvj.bravo7.form.impl.widget.CheckboxIntegerAdapter;
import org.kvj.bravo7.form.impl.widget.SpinnerIntegerAdapter;
import org.kvj.bravo7.form.impl.widget.TextViewStringAdapter;
import org.kvj.bravo7.form.impl.widget.TransientAdapter;
import org.kvj.whiskey2.R;
import org.kvj.whiskey2.Whiskey2App;
import org.kvj.whiskey2.data.DataController;
import org.kvj.whiskey2.data.NoteInfo;
import org.kvj.whiskey2.widgets.adapters.ColorsSpinnerAdapter;
import org.kvj.whiskey2.widgets.adapters.WidthsSpinnerAdapter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Spinner;

import com.actionbarsherlock.app.SherlockDialogFragment;

public class EditorDialogFragment extends SherlockDialogFragment {

	private static final String TAG = "Editor";

	private static final String KEY_ID = "id";
	private static final String KEY_COLLAPSIBLE = "collapsible";
	private static final String KEY_WIDTH = "width";
	private static final String KEY_COLOR = "color";
	private static final String KEY_TEXT = "text";

	public static EditorDialogFragment newInstance(NoteInfo info) {
		EditorDialogFragment f = new EditorDialogFragment();
		Bundle args = new Bundle();
		args.putLong(KEY_ID, info.id);
		args.putBoolean(KEY_COLLAPSIBLE, info.collapsible);
		args.putInt(KEY_WIDTH, info.width);
		args.putInt(KEY_COLOR, info.color);
		args.putString(KEY_TEXT, info.text);
		f.setArguments(args);
		return f;
	}

	private DataController controller;
	private FormController formController;

	public EditorDialogFragment() {
		super();
		this.controller = Whiskey2App.getInstance().getBean(DataController.class);
	}

	@Override
	public void onSaveInstanceState(Bundle data) {
		super.onSaveInstanceState(data);
		formController.save(data);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle("Note editor");
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.dialog_editor, null);
		formController = new FormController(view);
		formController.add(new TransientAdapter<Long>(new LongBundleAdapter(), -1L), KEY_ID);
		formController.add(new TextViewStringAdapter(R.id.edit_text, ""), KEY_TEXT);
		formController.add(new CheckboxIntegerAdapter(R.id.edit_collapsible, false), KEY_COLLAPSIBLE);
		final WidthsSpinnerAdapter widthsAdapter = new WidthsSpinnerAdapter(controller.getWidths());
		ViewBundleAdapter<Spinner, Integer> widthsFormAdapter = new ViewBundleAdapter<Spinner, Integer>(
				new IntegerBundleAdapter(), R.id.edit_width, controller.getWidths()[0]) {

			@Override
			public Integer getWidgetValue(Bundle bundle) {
				return widthsAdapter.getItem(getView().getSelectedItemPosition());
			}

			@Override
			public void setWidgetValue(Integer value, Bundle bundle) {
				widthsAdapter.setValue(getView(), value);
			}
		};
		formController.add(widthsFormAdapter, KEY_WIDTH);
		widthsFormAdapter.getView().setAdapter(widthsAdapter);
		ColorsSpinnerAdapter colorsAdapter = new ColorsSpinnerAdapter(controller.getColors());
		SpinnerIntegerAdapter colorsFormAdapter = new SpinnerIntegerAdapter(R.id.edit_color, 0);
		formController.add(colorsFormAdapter, KEY_COLOR);
		colorsFormAdapter.getView().setAdapter(colorsAdapter);
		formController.load(this, savedInstanceState);
		builder.setView(view);
		Button saveButton = (Button) view.findViewById(R.id.editor_save);
		Button removeButton = (Button) view.findViewById(R.id.editor_remove);
		Button cancelButton = (Button) view.findViewById(R.id.editor_cancel);
		if (-1 == formController.getValue(KEY_ID, Long.class)) {
			removeButton.setVisibility(View.GONE);
		} else {
			removeButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					onRemoveClick();
				}
			});
		}
		saveButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				onSaveClick();
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

	protected void onRemoveClick() {
		// TODO Auto-generated method stub

	}

	protected void onSaveClick() {
		// TODO Auto-generated method stub

	}

	private void onCancelClick() {
		if (formController.changed()) {
			SuperActivity.showQuestionDialog(getActivity(),
					"Dismiss changes?",
					"There are unsaved changes. Are you sure want to continue?",
					new Runnable() {

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
