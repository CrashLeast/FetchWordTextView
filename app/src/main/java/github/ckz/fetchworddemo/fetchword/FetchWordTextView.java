package github.ckz.fetchworddemo.fetchword;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chenkuizhi on 2018/5/10.
 * 可选择优化点：   1.截图采用单个控件截图
 *                  2.当触摸事件超出自己的范围，将触摸事件抛出给其他控件
 */

public class FetchWordTextView extends android.support.v7.widget.AppCompatEditText {
    //常量
    private final static String TAG = "FetchWordTextView";

    //参数
    private final static int DEFAULT_SELECT_WORD_BACKGROUND=0xff7091A1;//默认选中时的背景色
    private final long LONG_PRESS_TIME=200;
    private static final int DEFAULT_MAGNIFIER_WIDTH=120;//dp
    private static final int DEFAULT_MAGNIFIER_HEIGHT=35;//dp
    private static final int DEFAULT_VERTICAL_OFFSET=25;//dp
    private static final int DEFAULT_ARROW_HEIGHT=5;

    //全局变量
    private int mArrowHeight;
    private Activity mActivity;
    private int mMagnifierWidht;
    private int mMagnifierHeight;
    private int mVerticalOffset;
    private boolean mMagnifierEnable;
    private int MAX_WIDTH;
    private int MAX_HEIGHT;
    private float mLastX = -1f;
    private float mLastY = -1f;
    private FrameLayout mParentView;
    private BubbleImageView mIvMagnifier;
    private SpannableString mSpannableString;
    private String mLastSelectedWord;
    private String mSelectedWord;
    private OnWordSelectListener mOnWordSelectListener;
    private BackgroundColorSpan mBackgroundColorSpan = new BackgroundColorSpan(DEFAULT_SELECT_WORD_BACKGROUND);
    private List<Word> mWords;
    private Bitmap mSnapshot;

    public FetchWordTextView(Context context) {
        this(context,null);
    }

    public FetchWordTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FetchWordTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        final DisplayMetrics metrics = getResources().getDisplayMetrics();
        MAX_WIDTH=metrics.widthPixels;
        MAX_HEIGHT=metrics.heightPixels;
        mMagnifierWidht= (int) (DEFAULT_MAGNIFIER_WIDTH * metrics.density + 0.5f);
        mMagnifierHeight=(int) (DEFAULT_MAGNIFIER_HEIGHT * metrics.density + 0.5f);
        mVerticalOffset=(int) (DEFAULT_VERTICAL_OFFSET * metrics.density + 0.5f);
        mVerticalOffset=(int) (DEFAULT_VERTICAL_OFFSET * metrics.density + 0.5f);
        mArrowHeight=(int) (DEFAULT_ARROW_HEIGHT * metrics.density + 0.5f);

        mActivity= (Activity) context;
        mIvMagnifier=new BubbleImageView(mActivity);
        mParentView= (FrameLayout) mActivity.getWindow().getDecorView();
        FrameLayout.LayoutParams layoutParams=new FrameLayout.LayoutParams(mMagnifierWidht,mMagnifierHeight);
        mIvMagnifier.setLayoutParams(layoutParams);
    }

    public void setOnWordSelectListener(OnWordSelectListener listener) {
        mOnWordSelectListener = listener;
    }

    //重写该方法,不做任何处理，阻止长按的时候弹出上下文菜单
    @Override
    protected void onCreateContextMenu(ContextMenu menu) {

    }

    //设置不可编辑
    @Override
    public boolean getDefaultEditable() {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        if (mWords == null) {
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastX = -1f;
                mLastY = -1f;
                break;
            case MotionEvent.ACTION_MOVE:
                if(isLongPress(event)){
                    getParent().requestDisallowInterceptTouchEvent(true);
                    showMagnifier((int)(event.getRawX()),(int)(event.getRawY()));
                    mLastX = event.getX();
                    mLastY = event.getY();
                    trySelectWord(mLastX,mLastY);
                }
                break;
            case MotionEvent.ACTION_UP:
                if(mSelectedWord!=null){
                    closeMagnifier();

                    if (mOnWordSelectListener != null) {
                        mOnWordSelectListener.onFetchWordSelected(mSelectedWord);
                        mOnWordSelectListener.onFetchWordClose();
                    }
                }
            case MotionEvent.ACTION_CANCEL:
                mLastX = -1f;
                mLastY = -1f;
                clearSelectedWord();
                break;
        }
        return true;
    }

    private void showMagnifier(int pointX,int pointY) {
        //边界值修改
        if(pointX<0)pointX=0;
        if(pointY<0)pointY=0;
        if(pointX>MAX_WIDTH)pointX=MAX_WIDTH;
        if(pointY>MAX_HEIGHT)pointY=MAX_HEIGHT;

        int leftMargin=pointX-mMagnifierWidht/2;
        int topMargin=pointY-mMagnifierHeight-mVerticalOffset;
        if(leftMargin<0)leftMargin=0;
        if(leftMargin>MAX_WIDTH-mMagnifierWidht)leftMargin=MAX_WIDTH-mMagnifierWidht;

        FrameLayout.LayoutParams layoutParams;
        if(!mMagnifierEnable){
            mMagnifierEnable=true;
            layoutParams= (FrameLayout.LayoutParams) mIvMagnifier.getLayoutParams();
            layoutParams.leftMargin=leftMargin;
            layoutParams.topMargin=topMargin;
            mParentView.addView(mIvMagnifier,layoutParams);
        }else {
            layoutParams= (FrameLayout.LayoutParams) mIvMagnifier.getLayoutParams();
            layoutParams.leftMargin=leftMargin;
            layoutParams.topMargin=topMargin;
            mIvMagnifier.setLayoutParams(layoutParams);
        }
        showRegionBitmap(pointX,pointY);
    }

    private void showRegionBitmap(int pointX,int pointY){
        Bitmap bitmap=getRegionBitmap(pointX,pointY);
        mIvMagnifier.setImageBitmap(bitmap);
    }

    private void closeMagnifier() {
        mMagnifierEnable=false;
        mSnapshot=null;
        mParentView.removeView(mIvMagnifier);
    }

    public Bitmap getRegionBitmap(int pointX,int pointY){
        mActivity.getWindow().getDecorView().setDrawingCacheEnabled(true);
        mSnapshot=Bitmap.createBitmap(mActivity.getWindow().getDecorView().getDrawingCache());
        Bitmap regionBitmap=null;
        if(mSnapshot!=null) {
            int width=mMagnifierWidht*7/8;
            int height=mMagnifierHeight*7/8;
            int startX=pointX-width/2;
            int startY=pointY-height/2+mArrowHeight/2;
            if(startX<0)startX=0;
            if(startY<0)startY=0;
            if(startX>MAX_WIDTH-width)startX=MAX_WIDTH-width;
            if(startY>MAX_HEIGHT-height)startY=MAX_HEIGHT-height;
            regionBitmap= Bitmap.createBitmap(mSnapshot, startX, startY, width, height);
        }
        mActivity.getWindow().getDecorView().setDrawingCacheEnabled(false);
        return regionBitmap;
    }

    private boolean isLongPress(MotionEvent event){
        return event.getEventTime()-event.getDownTime()>LONG_PRESS_TIME;
    }

    private void trySelectWord(float pointX,float pointY) {
        Layout layout = getLayout();
        if (layout == null) {
            return;
        }
        int line  = layout.getLineForVertical(getScrollY() + (int)pointY);
        final int index = layout.getOffsetForHorizontal(line, (int)pointX);
        Word selectedWord = getWord(index);

        if (selectedWord != null) {
            if(mOnWordSelectListener!=null){
                mOnWordSelectListener.onFetchWordOpen();
            }
            mSelectedWord = getText().subSequence(selectedWord.getStart(), selectedWord.getEnd()).toString();
            if(mSelectedWord!=mLastSelectedWord) {
                mLastSelectedWord=mSelectedWord;
                clearSelectedWord();
                mSpannableString.setSpan(mBackgroundColorSpan,
                        selectedWord.getStart(), selectedWord.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                setText(mSpannableString);
            }
            if(mOnWordSelectListener!=null){
                mOnWordSelectListener.onFetchWordChange(mSelectedWord);
            }
        }
    }

    private void clearSelectedWord() {
        BackgroundColorSpan[] spans = mSpannableString.getSpans(0, getText().length(), BackgroundColorSpan.class);
        for (int i = 0; i < spans.length; i++) {
            mSpannableString.removeSpan(spans[i]);
        }
        setText(mSpannableString);
    }

    private Word getWord(final int index) {
        if (mWords == null) {
            return null;
        }

        for (Word w : mWords) {
            if (w.isIn(index)) {
                return w;
            }
        }

        return null;
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        mSpannableString = SpannableString.valueOf(getEditableText());
        mWords = getWords(text);
    }

    private List<Word> getWords(CharSequence s) {

        if (s == null) {
            return null;
        }

        List<Word> result = new ArrayList<>();

        int start = -1;

        int i = 0;

        for (; i < s.length(); i++) {
            char c = s.charAt(i);

            if(isLetter(c)||isConnector(c)){
                if (start == -1) {
                    start = i;
                }
            }else{
                if (start != -1) {
                    result.add(new Word(start, i));// From ( 0, 4 )
                }
                start = -1;
            }
        }

        if (start != -1) {
            result.add(new Word(start, i));
        }

        return result;

    }

    private boolean isLetter(char c){
        return (c>64&&c<91)||(c>96&&c<123);//(64,91)&&(96,123)区间对应a-z,A-Z的Ascii码值
    }

    private boolean isConnector(char c){
        return c=='\''||c=='-';
    }

    private class Word {
        public Word(final int start, final int end) {
            this.mStart = start;
            this.mEnd = end;
        }

        private int mStart;
        private int mEnd;

        public int getStart() {
            return this.mStart;
        }

        public int getEnd() {
            return this.mEnd;
        }

        public boolean isIn(final int index) {
            if (index >= getStart() && index <= getEnd()) {
                return true;
            }
            return false;
        }
    }

    public interface OnWordSelectListener {
        void onFetchWordOpen();
        void onFetchWordClose();
        void onFetchWordChange(String word);
        void onFetchWordSelected(String word);
    }
}
