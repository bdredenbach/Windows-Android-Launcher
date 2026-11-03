package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.*;
import android.util.LruCache;
import android.widget.ImageView;
import java.lang.ref.WeakReference;
import java.util.concurrent.*;

/** Small, permission-scoped stills. No disk image cache or background refresh. */
final class ExplorerThumbnails {
    private final Context context;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,2,15,TimeUnit.SECONDS,new ArrayBlockingQueue<>(32),new ThreadPoolExecutor.DiscardPolicy());
    private final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(4*1024*1024){protected int sizeOf(String key,Bitmap image){return image.getAllocationByteCount();}};
    private CancellationSignal signal=new CancellationSignal();
    private boolean closed;
    ExplorerThumbnails(Context context){this.context=context.getApplicationContext();}
    void bind(ImageView target,DocumentOps.Ref file){
        String key=file.uri+":"+file.item.modified+":"+file.item.size;target.setTag(key);
        Bitmap saved=cache.get(key);if(saved!=null){target.setImageBitmap(saved);return;}
        WeakReference<ImageView> reference=new WeakReference<>(target);CancellationSignal request=signal;
        worker.execute(()->{Bitmap bitmap=null;try{bitmap=PhotoImage.read(context,file.uri,request,192,40000).bitmap;}catch(Exception|OutOfMemoryError ignored){}
            Bitmap result=bitmap;main.post(()->{if(closed||request.isCanceled()){if(result!=null)result.recycle();return;}ImageView view=reference.get();if(result!=null){cache.put(key,result);if(view!=null&&key.equals(view.getTag())&&view.isAttachedToWindow())view.setImageBitmap(result);}});
        });
    }
    void clear(){signal.cancel();signal=new CancellationSignal();worker.getQueue().clear();cache.evictAll();}
    void close(){closed=true;clear();worker.shutdownNow();}
}
