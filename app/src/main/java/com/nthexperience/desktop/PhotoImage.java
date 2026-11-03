package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.*;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.*;
import java.io.*;

/** Bounded still-image decoding, off the UI thread, with EXIF rotation/reflection. */
final class PhotoImage {
    final Bitmap bitmap;
    final int width,height;
    PhotoImage(Bitmap bitmap,int width,int height){this.bitmap=bitmap;this.width=width;this.height=height;}
    static boolean picture(String name,String type){return FileLaunch.mime(name,type).startsWith("image/");}
    static PhotoImage read(Context context,Uri uri,CancellationSignal signal)throws IOException {
        return read(context,uri,signal,3072,6_000_000L);
    }
    static PhotoImage read(Context context,Uri uri,CancellationSignal signal,int longest,long pixels)throws IOException {
        if(!"content".equals(uri.getScheme()))throw new IOException("Choose a picture from Explorer.");
        BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
        decode(context,uri,options,signal);int width=options.outWidth,height=options.outHeight;
        if(width<=0||height<=0)throw new IOException("This picture's format is unsupported or its data is damaged.");
        int orientation=ExifInterface.ORIENTATION_NORMAL;
        try(android.content.res.AssetFileDescriptor file=context.getContentResolver().openAssetFileDescriptor(uri,"r",signal)){
            if(file!=null)try(InputStream in=file.createInputStream()){orientation=new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);}
        }catch(IOException ignored){}
        signal.throwIfCanceled();options.inJustDecodeBounds=false;options.inPreferredConfig=Bitmap.Config.ARGB_8888;options.inSampleSize=1;
        while((long)(width/options.inSampleSize)*(height/options.inSampleSize)>pixels||Math.max(width,height)/options.inSampleSize>longest)options.inSampleSize*=2;
        Bitmap bitmap=null;
        for(int retry=0;retry<3&&bitmap==null;retry++)try{bitmap=decode(context,uri,options,signal);}catch(OutOfMemoryError tooLarge){options.inSampleSize*=2;}
        if(bitmap==null)throw new IOException("This picture could not be decoded. Try Open with another app.");
        try{
            signal.throwIfCanceled();Matrix transform=new Matrix();
            switch(orientation){case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:transform.setScale(-1,1);break;case ExifInterface.ORIENTATION_ROTATE_180:transform.setRotate(180);break;case ExifInterface.ORIENTATION_FLIP_VERTICAL:transform.setScale(1,-1);break;case ExifInterface.ORIENTATION_TRANSPOSE:transform.setRotate(90);transform.postScale(-1,1);break;case ExifInterface.ORIENTATION_ROTATE_90:transform.setRotate(90);break;case ExifInterface.ORIENTATION_TRANSVERSE:transform.setRotate(-90);transform.postScale(-1,1);break;case ExifInterface.ORIENTATION_ROTATE_270:transform.setRotate(-90);break;}
            if(!transform.isIdentity()){Bitmap rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),transform,true);if(rotated!=bitmap)bitmap.recycle();bitmap=rotated;}
            boolean swapped=orientation>=5&&orientation<=8;
            return new PhotoImage(bitmap,swapped?height:width,swapped?width:height);
        }catch(RuntimeException|OutOfMemoryError error){bitmap.recycle();throw error;}
    }
    private static Bitmap decode(Context context,Uri uri,BitmapFactory.Options options,CancellationSignal signal)throws IOException {
        signal.throwIfCanceled();try(android.content.res.AssetFileDescriptor file=context.getContentResolver().openAssetFileDescriptor(uri,"r",signal)){
            if(file==null)throw new FileNotFoundException("Picture unavailable");
            try(InputStream in=file.createInputStream()){return BitmapFactory.decodeStream(in,null,options);}
        }
    }
}
