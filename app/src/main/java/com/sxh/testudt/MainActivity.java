package com.sxh.testudt;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.sxh.testudt.udt.util.ReceiveFile;
import com.sxh.testudt.udt.util.SendFile;

import java.io.FileInputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btnServer).setOnClickListener(this);
        findViewById(R.id.btnClient).setOnClickListener(this);

    }

    @Override
    public void onClick(View v)
    {
        int vId = v.getId();
        switch (vId)
        {
            case R.id.btnClient:
                onClient();
                break;
            case R.id.btnServer:
                onServer();
                break;
        }
    }

    private void onServer()
    {
        new Thread()
        {
            @Override
            public void run()
            {
                SendFile sf=new SendFile(65321);
                sf.run();
            }
        }.start();

    }

    private void onClient()
    {
        new Thread()
        {
            @Override
            public void run()
            {
                String sysESDir = Environment.getExternalStorageDirectory().getAbsolutePath();
                int serverPort=65321;
                String serverHost="localhost";
                String remoteFile=sysESDir+"/test.mp4";
                String localFile=sysESDir+"/test_down.mp4";
                ReceiveFile rf=new ReceiveFile(serverHost,serverPort,remoteFile, localFile);
                rf.run();
            }
        }.start();

    }
}
