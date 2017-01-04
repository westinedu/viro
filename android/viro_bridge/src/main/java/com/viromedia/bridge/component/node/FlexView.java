/*
 * Copyright © 2016 Viro Media. All rights reserved.
 */
package com.viromedia.bridge.component.node;

import android.content.Context;

import com.viro.renderer.jni.BaseGeometry;
import com.viro.renderer.jni.MaterialJni;
import com.viro.renderer.jni.SurfaceJni;

import java.util.List;

public class FlexView extends Node {

    private static final int COLOR_NOT_SET = -1;
    private static final int TRANSPARENT_COLOR = 0;
    private static final String DIFFUSE_COLOR_NAME  = "diffuseColor";

    // in accordance with how React Views behave. If you don't set a height/width on a FlexView then
    // it's width/height is 0.
    private float mWidth = 0;
    private float mHeight = 0;
    private int mBackgroundColor = -1;

    private final MaterialJni mDefaultMaterial;
    private SurfaceJni mNativeSurface;
    private MaterialJni mNativeColorMaterial;

    public FlexView(Context context) {
        super(context);
        mDefaultMaterial = new MaterialJni();
        mDefaultMaterial.setColor(TRANSPARENT_COLOR, DIFFUSE_COLOR_NAME);
        mDefaultMaterial.setWritesToDepthBuffer(true);
        mDefaultMaterial.setReadsFromDepthBuffer(true);
    }

    public void setWidth(float width) {
        mWidth = width;
    }

    public void setHeight(float height) {
        mHeight = height;
    }

    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
        setBackgroundOnSurface();
    }

    /**
     * Override setMaterials() of Node because we don't necessarily always want to use the given
     * Materials (ie. in the case a backgroundColor is also provided).
     */
    @Override
    protected void setMaterials(List<MaterialJni> materials) {
        mMaterials = materials;

        if (mBackgroundColor != COLOR_NOT_SET) {
            // only set background on surface if a color isn't set.
            setBackgroundOnSurface();
        }
    }

    /**
     * Override setGeometry because we don't want to always use the Materials set on a FlexView.
     */
    @Override
    protected void setGeometry(BaseGeometry geometry) {
        if (getNodeJni() != null) {
            getNodeJni().setGeometry(geometry);
        }
    }

    @Override
    public void onPropsSet() {
        super.onPropsSet();
        // Do nothing for "onPropsSet" on FlexViews because they are only ready AFTER they've
        // been laid out (see attemptReacalcLayout()).
    }

    @Override
    public void onTearDown() {
        if (isTornDown()) {
            return;
        }

        if (mNativeSurface != null) {
            mNativeSurface.destroy();
            mNativeSurface = null;
        }

        if (mNativeColorMaterial != null) {
            mNativeColorMaterial.destroy();
            mNativeColorMaterial = null;
        }

        mDefaultMaterial.destroy();
    }

    @Override
    protected void attemptRecalcLayout() {
        super.attemptRecalcLayout();

        // create the surface once we're sure that we've been laid out
        createSurface();
    }

    @Override
    protected float[] get2DPosition() {
        if (isRootFlexView()) {
            float[] arr = {getPivotX(), getPivotY()};
            return arr;
        } else {
            return super.get2DPosition();
        }
    }

    private boolean isRootFlexView() {
        return getParent() instanceof Scene || getParent() instanceof NodeContainer;
    }

    private void createSurface() {
        if (isTornDown()) {
            return;
        }

        if (mNativeSurface != null) {
            mNativeSurface.destroy();
        }

        mNativeSurface = new SurfaceJni(mWidth, mHeight);
        setBackgroundOnSurface();
        setGeometry(mNativeSurface);
    }

    /**
     * Helper function which will set a material on the mNativeSurface or remove material if neither
     * color nor materials are given.
     */
    private void setBackgroundOnSurface() {
        if (isTornDown() || mNativeSurface == null) {
            return;
        }

        if (mBackgroundColor != COLOR_NOT_SET) {
            // destroy the old color material
            if (mNativeColorMaterial != null) {
                mNativeColorMaterial.destroy();
            }
            mNativeColorMaterial = new MaterialJni();
            mNativeColorMaterial.setWritesToDepthBuffer(true);
            mNativeColorMaterial.setReadsFromDepthBuffer(true);
            mNativeColorMaterial.setColor(mBackgroundColor, DIFFUSE_COLOR_NAME);
            mNativeSurface.setMaterial(mNativeColorMaterial);
        } else if (mMaterials != null && mMaterials.size() > 0) {
            // set the first material on the surface
            mNativeSurface.setMaterial(mMaterials.get(0));
        } else {
            if (mNativeColorMaterial != null) {
                mNativeColorMaterial.destroy();
                mNativeColorMaterial = null;
            }
            // set the default (transparent) material if no color or material is given
            mNativeSurface.setMaterial(mDefaultMaterial);
        }
    }

}