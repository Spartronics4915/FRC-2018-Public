package com.spartronics4915.frc2019.vision;

import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTable;

import com.spartronics4915.frc2019.Constants;
import com.spartronics4915.frc2019.vision.TargetInfo;

import com.spartronics4915.frc2019.vision.GoalTracker;


import com.spartronics4915.lib.util.CrashTrackingRunnable;
import com.spartronics4915.lib.util.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;


/**
 * (VisionLoop)
 *  
 * Responsible for polling NetworkTables and receiving vision updates
 * 
 * SmartDashboard ->
 * 
 * This controls all vision actions, including vision updates, capture, and interfacing with the Rasberry Pi with
 * NetworkTables. It also stores all VisionUpdates (from the Pi) and contains methods to add to/prune
 * the VisionUpdate list. Much like the subsystems, outside methods get the VisionServer instance (there is only one
 * VisionServer) instead of creating new VisionServer instances.
 * 
 * @see VisionUpdate.java
 */

public class VisionServer extends CrashTrackingRunnable {

    private static VisionServer s_instance = null;
    private boolean m_running = true;
    double lastMessageReceivedTime = 0;
    private boolean m_use_java_time = false;    //Read: Comment at end of file
    private volatile boolean mWantsAppRestart = false;

    private NetworkTableEntry mVisionTargetAngleEntry = null;

    mVisionTargetAngleEntry = table.getEntry(Constants.kVisionTargetAngleName);
    mVisionTargetAngleEntry.forceSetNumber(0);

    public static VisionServer getInstance() {
        if (s_instance == null) {
            s_instance = new VisionServer();
        }
        return s_instance;
    }

    public void requestAppRestart() {
        mWantsAppRestart = true;
    }
    // REVIEW:  Currently, ServerThread is a thread within the VisionServer object, which is a thread.
    //          I would assume this is bad, and should be fixed
    protected class ServerThread extends CrashTrackingRunnable {

        public ServerThread() {
            // Insert smartDashboard key shenanigins
        }

        public void generateVisionReading(double timestamp) {
            // Reading table 
            mVisionTargetAngleEntry = table.getEntry(Constants.kVisionTargetAngleName);
            mVisionTargetElevationEntry = table.getEntry(Constants.kVisionTargetElevationName);

            double dx = mVisionTargetAngleEntry.getNumber(0).doubleValue();
            // TODO: Change to reflect targetInfo
            double dy = mVisionTargetElevationEntry.getNumber(0).doubleValue();

            return TargetInfo target = new TargetInfo(dx,dy);

        }

        public boolean isAlive() {
            return m_socket != null && m_socket.isConnected() && !m_socket.isClosed();
        }

        @Override
        // First instance of reading the message
        // Move to onLoop()
        public void runCrashTracked() {
            String status = table.getEntry(Constants.kVisionStatusName);
            try {

                // TODO: Make this while() loop more robust
                while (status != null){

                    double timestamp = getTimestamp();
                    
                    lastMessageReceivedTime = timestamp;
                    
                    TargetInfo update = generateVisionReading()
                }    

                System.out.println("Socket disconnected");
            } catch (IOException e) {
                System.err.println("Could not talk to socket");
            }
            if (m_socket != null) {
                try {
                    m_socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Instantializes the VisionServer.
     *
     */
    private VisionServer() {}

        final NetworkTable table = NetworkTableInstance.getDefault().getTable(Constants.kVisionTableName);
        // This is the clever location where you implement initalizing based around smartDashboard keys
        new Thread(this).start();
    }

    @Override
    public void runCrashTracked() {
        while (m_running) {
            try {
                ServerThread s = new ServerThread();
                new Thread(s).start();
            } catch (IOException e) {
                System.err.println("Issue starting vision server thread!");
            } finally {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // This time buisness is being handwaved ATM
    private double getTimestamp() {
        if (m_use_java_time) {
            return System.currentTimeMillis();
        } else {
            return Timer.getFPGATimestamp();
        }
    }
}

/*

        mVisionTargetAngleEntry = table.getEntry(Constants.kVisionTargetAngleName);
mVisionTargetAngleEntry.forceSetNumber(0);
*/