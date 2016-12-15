package fh.praxisphase.fahrzeugsteuerung.utility;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import fh.praxisphase.fahrzeugsteuerung.R;

/**
 * Erzeugt einen Schieberegler zur Steuerung der Geschwindigkeit oder Lenkung
 */
public class SlideControl extends View {
    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final String TAG = "SlideControl";
    private final int HORIZONTAL = 0;
    private final int VERTICAL = 1;
    private int orientation;
    private int positions;
    private int sliderSize;
    private float sliderPosition;
    private int sliderColor;
    private int controlerColor;
    private boolean invert;

    public SlideControl(Context context) {
        super(context);

        init();
    }

    public SlideControl(Context context, AttributeSet attrs) {
        super(context, attrs);

        init(context, attrs);
    }

    public SlideControl(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs);
    }

    /**
     * Initialisiert den Slider mit Default Werten
     * */
    private void init(){
        orientation = VERTICAL;
        positions = 25;
        sliderSize = 100;
        sliderPosition = 0;
        sliderColor = Color.RED;
        controlerColor = Color.RED;
        invert = false;
    }

    /**
     * Initialisert den Slider mit den Werten aus der Layout Datei
     *
     * @param context Der verwendete Context
     * @param attributeSet Die Werte aus der Layout Datei*/
    private void init(Context context, AttributeSet attributeSet){
        TypedArray attribute = context.getTheme().obtainStyledAttributes(attributeSet, R.styleable.SlideControl, 0, 0);

        try{
            orientation = attribute.getInt(R.styleable.SlideControl_sliderOrientation, VERTICAL);
            positions = attribute.getInt(R.styleable.SlideControl_anzahlPositionen, 25);
            sliderSize = attribute.getInt(R.styleable.SlideControl_sliderSize, 100);
            sliderPosition = 0;
            sliderColor = attribute.getColor(R.styleable.SlideControl_sliderColor, Color.RED);
            controlerColor = attribute.getColor(R.styleable.SlideControl_controlerColor, Color.RED);
            invert = false;
        } finally{
            attribute.recycle();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(sliderPosition == 0){
            resetSlider();
        }

        if (orientation == HORIZONTAL) {
            drawHorizontalSlider(canvas);
        } else {
            drawVerticalSlider(canvas);
        }
    }

    /**
     * Ändert den Wertebereich.
     *
     * @param positions Anzahl möglicher Werte
     * */
    public void setPositions(int positions){
        this.positions = positions;
    }

    /**
     * Legt fest, ob das Ergebnis vom Schieberegler invertiert werden soll.
     *
     * @param invert Wird invertiert oder nicht
     * */
    public void invertSliderOutput(boolean invert){
        this.invert = invert;
    }

    /**
     * Zeichnet einen Horizontalen Schieberegler
     *
     * @param canvas Das Canvas auf das gezeichnet werden soll
     * */
    private void drawHorizontalSlider(Canvas canvas){
        float strokeWidth = 2;
        int width = getWidth()-(int)strokeWidth;
        int height = getHeight()-(int)strokeWidth;

        Paint paint = new Paint();
//        Umrandung vom Schieberegler
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(controlerColor);
        paint.setStrokeWidth(strokeWidth);
        canvas.drawRect(getTranslationX(), getTranslationY(), getTranslationX()+width, getTranslationY()+height, paint);

//         Mittellinie
        paint.setColor(Color.WHITE);
        canvas.drawLine(width/2, getTranslationY(), width/2, getRotationY()+height, paint);

//        Regler
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(sliderColor);
        canvas.drawRect(sliderPosition-sliderSize/2, getTranslationY(), sliderPosition+sliderSize/2, getTranslationY()+width, paint);

//        Regler Mittellinie
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        canvas.drawLine(sliderPosition, getTranslationY(), sliderPosition, getTranslationY()+height, paint);
    }

    /**
     * Zeichnet einen Vertikalen Schieberegler
     *
     * @param canvas Das Canvas auf das gezeichnet werden soll
     * */
    private void drawVerticalSlider(Canvas canvas){
        float strokeWidth = 2;
        int width = getWidth()-(int)strokeWidth;
        int height = getHeight()-(int)strokeWidth;

        Paint paint = new Paint();
//        Umrandung vom Schieberegler
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(controlerColor);
        paint.setStrokeWidth(strokeWidth);
        canvas.drawRect(getTranslationX(), getTranslationY(), getTranslationX()+width, getTranslationY()+height, paint);

//        Mittellinie
        paint.setColor(Color.WHITE);
        canvas.drawLine(getTranslationX(), height/2, getTranslationX()+width, height/2, paint);

//        Regler
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(sliderColor);
        canvas.drawRect(getTranslationX(), sliderPosition-sliderSize/2, getTranslationX()+width, sliderPosition+sliderSize/2, paint);

//        Regler Mittellinie
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        canvas.drawLine(getTranslationX(), sliderPosition, getTranslationX()+width, sliderPosition, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(orientation == HORIZONTAL){
            if(!horizontalSliderTouchEvent(event)){
                return super.onTouchEvent(event);
            }
        } else{
            if(!verticalSliderTouchEvent(event)){
                return super.onTouchEvent(event);
            }
        }

        return true;
    }

    /**
     * Bewegt den Horizontalen Schieberegler entsprechend der Fingerposition
     *
     * @param event Das aufgetretene MotionEvent vom Schieberegler
     * */
    private boolean horizontalSliderTouchEvent(MotionEvent event){
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (event.getX() < sliderPosition + sliderSize / 2
                        && event.getX() > sliderPosition - sliderSize / 2) {
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (sliderPosition != event.getX()
                        && event.getX() + sliderSize / 2 < getWidth()
                        && event.getX() - sliderSize / 2 > getTranslationX()) {
                    sliderPosition = (int) event.getX();
                    invalidate();
                }
                else if(sliderPosition != event.getX()
                        && event.getX() + sliderSize / 2 < getWidth()){
                    sliderPosition = sliderSize/2;
                    invalidate();
                }
                  else if(sliderPosition != event.getX()
                        && event.getX() - sliderSize / 2 > getTranslationX()){
                    sliderPosition = getWidth() - sliderSize/2;
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                resetSlider();
                invalidate();
                break;
        }
        return false;
    }

    /**
     * Bewegt den Vertikalen Schieberegler entsprechend der Fingerposition
     *
     * @param event Das aufgetretene MotionEvent vom Schieberegler
     * */
    private boolean verticalSliderTouchEvent(MotionEvent event){
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                if(event.getY() < sliderPosition+sliderSize/2 && event.getY() > sliderPosition-sliderSize/2) {
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(sliderPosition != event.getY()
                        && event.getY()+sliderSize/2 < getHeight()
                        && event.getY()-sliderSize/2 > getTranslationY()) {
                    sliderPosition = event.getY();
                    invalidate();
                }
                else if(sliderPosition != event.getY()
                        && event.getY() + sliderSize / 2 < getHeight()){
                    sliderPosition = sliderSize/2;
                    invalidate();
                } else if(sliderPosition != event.getY()
                        && event.getY() - sliderSize / 2 > getTranslationY()) {
                    sliderPosition = getHeight() - sliderSize/2;
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                resetSlider();
                invalidate();
                break;
        }

        return false;
    }

    /**
     * Liefert die aktuelle Stellung des Reglers zurück.
     *
     * @return Wert befindet sich im positiven oder negativen Wertebereich
     */
    public int getPosition(){
        if(orientation == VERTICAL){
            if(invert){
                return getVerticalSliderPosition() * -1;
            }
            return getVerticalSliderPosition();
        } else{
            if(invert){
                return getHorizontalSliderPosition() * -1;
            }
            return getHorizontalSliderPosition();
        }
    }

    /**
     * Gibt die Reglerstellung von einem Horizontalen Schieberegler zurück
     *
     * @return Die Position vom Regler
     * */
    private int getHorizontalSliderPosition() {
//        Berechnet aus der Weite vom Schieberegler, der Größe vom Schieberegler und der Position
//        einen Wert aus dem Wertebereich
        float faktor = positions / (float)((getWidth() - sliderSize) / 2);

        if(sliderPosition < getWidth()/2){
            return (int)(positions -((sliderPosition - sliderSize/2)*faktor));
        } else if(sliderPosition == getWidth()/2){
            return 0;
        } else{
            return (int)((((sliderPosition-(sliderSize/2))-((getWidth()-sliderSize)/2)) * faktor) * -1);
        }
    }

    /**
     * Gibt die Reglerstellung von einem Vertikalen Schieberegler zurück
     *
     * @return Die Position vom Regler
     * */
    private int getVerticalSliderPosition() {
//        Berechnet aus der Höhe vom Schieberegler, der Größe vom Schieberegler und der Position
//        einen Wert aus dem Wertebereich
        float faktor = positions / (float)((getHeight() - sliderSize) / 2);

        if(sliderPosition < getHeight()/2){
            return (int)(positions -((sliderPosition - sliderSize/2)*faktor));
        } else if(sliderPosition == getHeight()/2){
            return 0;
        } else{
            return (int)(((sliderPosition-(sliderSize/2))-((getHeight()-sliderSize)/2)) * faktor) * -1;
        }
    }

    /**
     * Stellt den Regler zurück auf die neutrale Position, welche sich in der Mitte befindet.
     */
    public void resetSlider(){
        if(orientation == HORIZONTAL){
            sliderPosition = getTranslationX()+getWidth()/2;
        } else{
            sliderPosition = getTranslationY()+getHeight()/2;
        }
        invalidate();
    }
}