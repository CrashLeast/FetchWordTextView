package github.ckz.fetchworddemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import github.ckz.fetchworddemo.fetchword.FetchWordTextView;

public class MainActivity extends AppCompatActivity {

    private FetchWordTextView mFwtvSentence;
    private TextView mTvWord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFwtvSentence=findViewById(R.id.fwtv_sentence);
        mTvWord=findViewById(R.id.tv_word);

        mFwtvSentence.setOnWordSelectListener(new FetchWordTextView.OnWordSelectListener() {

            @Override
            public void onFetchWordOpen() {
            }

            @Override
            public void onFetchWordClose() {
            }

            @Override
            public void onFetchWordChange(String word) {

            }

            @Override
            public void onFetchWordSelected(String word) {
                mTvWord.setText(word);
            }


        });

    }
}
