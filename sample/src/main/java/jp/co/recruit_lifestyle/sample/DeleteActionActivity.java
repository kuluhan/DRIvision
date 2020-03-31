package jp.co.recruit_lifestyle.sample;

import android.os.Bundle;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;

import jp.co.recruit.floatingview.R;
import jp.co.recruit_lifestyle.sample.fragment.DeleteActionFragment;

public class DeleteActionActivity extends AppCompatActivity {

    private static final String FRAGMENT_TAG_DELETE_ACTION = "delete_action";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_action);

        if (savedInstanceState == null) {
            final FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.container, DeleteActionFragment.newInstance(), FRAGMENT_TAG_DELETE_ACTION);
            ft.commit();
        }

    }
}