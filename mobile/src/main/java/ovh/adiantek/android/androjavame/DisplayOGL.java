package ovh.adiantek.android.androjavame;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Parcel;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.MotionEvent;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.microedition.khronos.opengles.GL10;
import java.util.ArrayList;

public class DisplayOGL extends GLSurfaceView implements GLSurfaceView.Renderer
{
    public int screenWidth=-1;
    public int screenHeight;
    public DisplayOGL(Context c){
        super(c);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

    }
    public void onStartRenderer(){}
    private TreeMap<Integer,Layer> mapa = new TreeMap<Integer,Layer>();
    public SparseArray<PointF> pointers =new SparseArray<PointF>();
    public boolean onTouchEvent(MotionEvent event) {
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);
        int maskedAction = event.getActionMasked();
        switch (maskedAction) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                PointF f = new PointF();
                f.x = event.getX(pointerIndex);
                f.y = event.getY(pointerIndex);
                pointers.put(pointerId, f);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                for (int size = event.getPointerCount(), i = 0; i < size; i++) {
                    PointF point = pointers.get(event.getPointerId(i));
                    if (point != null) {
                        point.x = event.getX(i);
                        point.y = event.getY(i);
                    }
                }
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {
                pointers.remove(pointerId);
                break;
            }
        }
        return true;
    }
    public void onSurfaceCreated(GL10 gl, javax.microedition.khronos.egl.EGLConfig p2)
    {
        screenWidth=getWidth();
        screenHeight=getHeight();
        onStartRenderer();

        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA , GL10.GL_ONE_MINUS_SRC_ALPHA);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);
        gl.glClearDepthf(1.0f);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
    }
    public void onSurfaceChanged(GL10 p1, int p2, int p3)
    {
        p1.glViewport(0, 0, p2, p3);
    }
    public void onDrawFrame(GL10 gl)
    {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        for(Map.Entry<Integer,Layer> m :mapa.entrySet()){
            m.getValue().beforeDrawing();
            m.getValue().renderToGL(gl);
            m.getValue().afterDrawing();
        }
    }
    private void d(int l){
        mapa.get(l)._a();
    }
    public void clearLayers(){for(int i=0; i<mapa.size(); i++)d(i);mapa.clear();}
    public boolean containsLevel(int p){return mapa.containsKey(p);}
    public boolean containsLayer(Layer p){return mapa.containsValue(p);}
    public Set<Map.Entry<Integer,Layer>> entrySet(){return mapa.entrySet();}
    public Layer getLayer(int p){return mapa.get(p);}
    public boolean hasLayers(){return mapa.size()!=0;}
    public Layer addLayer(int level, Layer layer){layer._a(this);return mapa.put(level,layer);}
    public void putAll(java.util.Map<? extends Integer, ? extends Layer> p){for(Layer l : p.values())l._a(this);mapa.putAll(p);}
    public Layer removeLayer(int p){return mapa.remove(p);}
    public int sizeOfLayers(){return mapa.size();}
    public Collection<Layer> layers(){return mapa.values();}

    public static class Layer extends Canvas
    {
        private static final FloatBuffer vertexBuffer;
        private static final FloatBuffer textureBuffer;
        private Bitmap b;
        private static long id=0;
        private final long this_id;
        public Layer(int width, int height){
            b=(Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888));
            this_id=id;
            id++;
            setBitmap(b);
        }

        public void beforeDrawing(){}
        public void afterDrawing(){}
        public final Bitmap getBitmap(){
            return b;
        }
        public final long getCustomID(){
            return this_id;
        }
        public void renderToGL(GL10 gl) {
            gl.glRotatef(0.01f,0,0,0);
            int[] textures = new int[1];
            gl.glGenTextures(1, textures, 0);
            gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_NEAREST);
            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, b, 0);
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
            gl.glFrontFace(GL10.GL_CW);
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vertexBuffer);
            gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, textureBuffer);
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
            gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
            gl.glDeleteTextures(1,textures,0);
        }
        public PointF[] getTouches(){
            SparseArray<PointF> p = gl.pointers.clone();
            double scaleX = getWidth()*1.0d/gl.getWidth();
            double scaleY = getHeight()*1.0d/gl.getHeight();
            ArrayList<PointF> pD =new ArrayList<PointF>();
            for(int i=0;i<p.size();i++){
                try {
                    PointF touches=new PointF();
                    touches.x=(float)(p.get(i).x*scaleX);
                    touches.y=(float)(p.get(i).y*scaleY);
                    pD.add(touches);
                }catch(Throwable t){}
            }
            return pD.toArray(new PointF[pD.size()]);
        }
        private DisplayOGL gl;
        private void _a(DisplayOGL d){
            if(gl!=null)throw new RuntimeException("Layout is already using by instance "+gl+". Remove this instance before adding to other.");
            gl=d;
        }
        public DisplayOGL getParent(){
            return gl;
        }
        private void _a(){
            gl=null;
        }
        public void setBitmap(Bitmap b){this.b=b;super.setBitmap(b);}
        public int getDensity() {return b.getDensity();}
        public void setDensity(int density) {b.setDensity(density);}
        public void recycle() {b.recycle();}
        public boolean isRecycled() {return b.isRecycled();}
        public void copyPixelsToBuffer(Buffer dst) {b.copyPixelsToBuffer(dst);}
        public void copyPixelsFromBuffer(Buffer src) {b.copyPixelsFromBuffer(src);}
        public Bitmap copy(Bitmap.Config config, boolean isMutable) {return b.copy(config,isMutable);}
        public byte[] getNinePatchChunk() {return b.getNinePatchChunk();}
        public boolean compress(Bitmap.CompressFormat format, int quality, OutputStream stream) {return b.compress(format,quality,stream);}
        public boolean isMutable() {return b.isMutable();}
        public int getWidth() {return b.getWidth();}
        public int getHeight() {return b.getHeight();}
        public int getScaledWidth(Canvas canvas) {return b.getScaledWidth(canvas);}
        public int getScaledHeight(Canvas canvas) {return b.getScaledHeight(canvas);}
        public int getScaledWidth(DisplayMetrics metrics) {return b.getScaledWidth(metrics);}
        public int getScaledHeight(DisplayMetrics metrics) {return b.getScaledHeight(metrics);}
        public int getScaledWidth(int targetDensity) {return b.getScaledWidth(targetDensity);}
        public int getScaledHeight(int targetDensity) {return b.getScaledHeight(targetDensity);}
        public int getRowBytes() {return b.getRowBytes();}
        public Bitmap.Config getConfig() {return b.getConfig();}
        public boolean hasAlpha() {return b.hasAlpha();}
        public void eraseColor(int c) {b.eraseColor(c);}
        public int getPixel(int x, int y) {return b.getPixel(x,y);}
        public void getPixels(int[] pixels, int offset, int stride, int x, int y, int width, int height) {b.getPixels(pixels,offset,stride,x,y,width,height);}
        public void setPixel(int x, int y, int color) {b.setPixel(x,y,color);}
        public void setPixels(int[] pixels, int offset, int stride, int x, int y, int width, int height) {b.setPixels(pixels,offset,stride,x,y,width,height);}
        public int describeContents() {return b.describeContents();}
        public void writeToParcel(Parcel p, int flags) {b.writeToParcel(p,flags);}
        public Bitmap extractAlpha() {return b.extractAlpha();}
        public Bitmap extractAlpha(Paint paint, int[] offsetXY) {return b.extractAlpha(paint,offsetXY);}
        public void prepareToDraw() {b.prepareToDraw();}
        static {
            ByteBuffer vertexByteBuffer = ByteBuffer.allocateDirect(128);
            vertexByteBuffer.order(ByteOrder.nativeOrder());
            vertexBuffer = vertexByteBuffer.asFloatBuffer();
            vertexBuffer.put(new float[]{
                    -1f, -1f, 0.0f,
                    -1f,  1f, 0.0f,
                    1f, -1f, 0.0f,
                    1f,  1f, 0.0f
            });
            vertexBuffer.position(0);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(128);
            byteBuffer.order(ByteOrder.nativeOrder());
            textureBuffer = byteBuffer.asFloatBuffer();
            textureBuffer.put(new float[]{
                    0.0f, 1.0f,
                    0.0f, 0.0f,
                    1.0f, 1.0f,
                    1.0f, 0.0f });
            textureBuffer.position(0);
        }
    }
}