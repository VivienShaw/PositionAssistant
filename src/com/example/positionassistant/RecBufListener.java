package com.example.positionassistant;

import java.io.FileNotFoundException;

public interface RecBufListener {
   public void onRecBufFull(byte[] data);
   public void register(RecBuffer r);
}
