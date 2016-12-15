package fh.praxisphase.fahrzeugsteuerung.utility;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Diese Klasse fügt eine Trennungslinie zwischen jeden Eintrag im RecyclerView ein.
 */
public class ItemDivider extends RecyclerView.ItemDecoration {
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final String TAG = "ItemDivider";
    Drawable divider;

    public  ItemDivider(Drawable divider){
        this.divider = divider;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        if(parent.getChildAdapterPosition(view) == 0){
            return;
        }

        outRect.top = divider.getIntrinsicHeight();
    }

    @Override
    public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        int paddingLeft = parent.getPaddingLeft();
        int paddingRight = parent.getWidth()-parent.getPaddingRight();

        int childCount = parent.getChildCount();

        for(int i = 0; i < childCount - 1; i++){
            View child = parent.getChildAt(i);

            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams)child.getLayoutParams();

            int dividerTop = child.getBottom()+ layoutParams.bottomMargin;
            int dividerBottom = dividerTop + divider.getIntrinsicHeight();

            divider.setBounds(paddingLeft, dividerTop, paddingRight, dividerBottom);
            divider.draw(canvas);
        }
    }
}