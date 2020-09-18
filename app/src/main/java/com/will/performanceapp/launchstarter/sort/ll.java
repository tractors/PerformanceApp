package com.will.performanceapp.launchstarter.sort;

import java.util.ArrayList;
import java.util.List;

public class ll {
    //顶点数
    private int mVerticeCount;
    //邻接表
    private ArrayList<Integer>[] mAdj;

    public ll(int verticeCount) {
        this.mVerticeCount = verticeCount;
        mAdj = new ArrayList[mVerticeCount];
        for (int i = 0; i < mVerticeCount; i++) {
            mAdj[i] = new ArrayList<Integer>();
        }
    }
}