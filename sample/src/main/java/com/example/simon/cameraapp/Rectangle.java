package com.example.simon.cameraapp;

public class Rectangle implements Comparable<Rectangle> {
    private int x ;
    private int y ;
    private int w;
    private int h;
    private int area;
    private String type;


    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getW() {
        return w;
    }

    public int getH() {
        return h;
    }
public String getType(){return type;}
    public Rectangle(String type,int x, int y, int w, int h,float prob) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.type=type;
        area=w*h;
    }

    public int getArea() {
        return area;
    }

    @Override
    public int compareTo(Rectangle o) {
       return(Integer.compare(o.getX(), this.x));
    }
}
