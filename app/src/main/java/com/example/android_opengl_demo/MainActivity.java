package com.example.android_opengl_demo;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.opengl.GLSurfaceView;

import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity {
    private static final String[] OBJECT_VERTEX_SHADER_CODE =
            new String[] {
                    "uniform mat4 u_MVP;",
                    "attribute vec4 a_Position;",
                    "attribute vec2 a_UV;",
                    "varying vec2 v_UV;",
                    "",
                    "void main() {",
                    "  v_UV = a_UV;",
                    "  gl_Position = u_MVP * a_Position;",
                    "}",
            };
    private static final String[] OBJECT_FRAGMENT_SHADER_CODE =
            new String[] {
                    "// This determines how much precision the GPU uses when calculating floats",
                    "precision mediump float;",
                    "varying vec2 v_UV;",
                    "uniform sampler2D u_Texture;",
                    "",
                    "void main() {",
                    "  // The y coordinate of this sample's textures is reversed compared to",
                    "  // what OpenGL expects, so we invert the y coordinate.",
                    "  gl_FragColor = texture2D(u_Texture, vec2(v_UV.x, 1.0 - v_UV.y));",
                    "}",
            };

    private static final float Z_NEAR = 0.01f;
    private static final float Z_FAR = 10.0f;
    private static final float DEFAULT_FLOOR_HEIGHT = -1.6f;

    private int objectProgram;
    private int objectPositionParam;
    private int objectUvParam;
    private int objectModelViewProjectionParam;

    private TexturedMesh room;
    private Texture roomTex;

    private float[] modelMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] mvpMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16];

    private GLSurfaceView gLView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity.
        gLView = new GLSurfaceView(this);
        // Create an OpenGL ES 2.0-compatible context.
        gLView.setEGLContextClientVersion(2);
        // The renderer is responsible for making OpenGL calls to render a frame.
        gLView.setRenderer(new GLSurfaceView.Renderer() {
            /*
            Called when the surface is created or recreated.
            Load model and texture here.
             */
            @Override
            public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
                // Set the background frame color
                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

                // Compile shaders
                objectProgram = Util.compileProgram(OBJECT_VERTEX_SHADER_CODE, OBJECT_FRAGMENT_SHADER_CODE);

                // Get handles to shader parameters
                objectPositionParam = GLES20.glGetAttribLocation(objectProgram, "a_Position");
                objectUvParam = GLES20.glGetAttribLocation(objectProgram, "a_UV");
                objectModelViewProjectionParam = GLES20.glGetUniformLocation(objectProgram, "u_MVP");

                // Set model transformation matrix
                Matrix.setIdentityM(modelMatrix, 0);
                Matrix.translateM(modelMatrix, 0, 0, DEFAULT_FLOOR_HEIGHT, 0);

                // Load objects and textures
                try {
                    room = new TexturedMesh(MainActivity.this, "CubeRoom.obj", objectPositionParam, objectUvParam);
                    roomTex = new Texture(MainActivity.this, "CubeRoom_BakedDiffuse.png");
                } catch (IOException e) {
                    Log.e("Renderer", "Unable to initialize objects", e);
                }
            }

            /*
            Called after the surface is created and whenever the OpenGL ES surface size changes.
            Typically you will set your viewport here.
            If your camera is fixed then you could also set your projection matrix here.
             */
            @Override
            public void onSurfaceChanged(GL10 gl10, int width, int height) {
                // Set viewport
                GLES20.glViewport(0, 0, width, height);

                // Calculate projection matrix parameters based on screen aspect ratio and FoV
                float ratio = (float) width / height;
                float left = -Z_NEAR * 1.732f, right = Z_NEAR * 1.732f;
                float top = right / ratio, bottom = left / ratio;

                // Calculate the projection matrix
                // This projection matrix is applied to object coordinates in the onDrawFrame() method
                Matrix.frustumM(projectionMatrix, 0, left, right, bottom, top, Z_NEAR, Z_FAR);
            }

            /*
            Called to draw the current frame.
            Calculate MVP transformation and draw the objects.
             */
            @Override
            public void onDrawFrame(GL10 gl10) {
                // Redraw background color
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

                // Enable depth test
                GLES20.glEnable(GLES20.GL_DEPTH_TEST);

                // Set the camera position (view matrix)
                Matrix.setLookAtM(viewMatrix, 0, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f);

                // Calculate the MVP transformation matrix
                Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
                Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);

                // bind parameters for shaders
                GLES20.glUseProgram(objectProgram);
                GLES20.glUniformMatrix4fv(objectModelViewProjectionParam, 1, false, mvpMatrix, 0);
                roomTex.bind();

                // draw objects
                room.draw();
            }
        });

        setContentView(gLView);
    }
}