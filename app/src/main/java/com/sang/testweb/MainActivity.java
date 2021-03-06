package com.sang.testweb;

import androidx.appcompat.app.AppCompatActivity;


import android.content.Context;
import android.media.AudioManager;
import android.media.MediaCodec;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.VideoView;
import org.webrtc.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {
///root/sockettest/socketFiles/test_socket.js
    private static final String SIGNALING_URI = "https://joylab.one:3355";
    private static final String VIDEO_TRACK_ID = "video1";
    private static final String AUDIO_TRACK_ID = "audio1";
    private static final String LOCAL_STREAM_ID = "stream1";
    private static final String SDP_MID = "sdpMid";
    private static final String SDP_M_LINE_INDEX = "sdpMLineIndex";
    private static final String SDP = "sdp";
    private static final String CREATEOFFER = "createoffer";
    private static final String OFFER = "offer";
    private static final String ANSWER = "answer";
    private static final String CANDIDATE = "candidate";
    private static final String ROOMNAME = "roomname";


    PeerConnectionFactory peerConnectionFactory;
    VideoSource localVideoSource;
    MediaStream localMediaStream;
    VideoRenderer otherPeerRenderer;
    PeerConnection peerConnection;
    String roomname = "12345";
    String Myplatform = "app";
    String platform="app";
    String web ="web";
    String app = "app";


    private Socket socket;
    private boolean createOffer = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);

        initWebRTC();

    }

    // Web RTC ??????
    public void initWebRTC(){
        PeerConnectionFactory.initializeAndroidGlobals(this,true,true,true,null);
        peerConnectionFactory = new PeerConnectionFactory();



        VideoCapturerAndroid vc = VideoCapturerAndroid.create(VideoCapturerAndroid.getNameOfFrontFacingDevice());
        //
        localVideoSource = peerConnectionFactory.createVideoSource(vc, new MediaConstraints());
        VideoTrack localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID,localVideoSource);
        localVideoTrack.setEnabled(true);

        AudioSource audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
        AudioTrack localAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID,audioSource);
        localAudioTrack.setEnabled(true);

        localMediaStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID);
        localMediaStream.addTrack(localVideoTrack);
        localMediaStream.addTrack(localAudioTrack);



        // GL ????????? ??? ??????.
        GLSurfaceView videoView = (GLSurfaceView) findViewById(R.id.view_Call);
        // ??????????????? ????????? ??????
        VideoRendererGui.setView(videoView , null);
        try{
            // ????????? ?????? ??????????????? ??? ??????????????????.
            otherPeerRenderer = VideoRendererGui.createGui(0,0,100,100,VideoRendererGui.ScalingType.SCALE_ASPECT_FILL , true);
            // ????????? ( ????????? ?????? ?????? )
            VideoRenderer renderer = VideoRendererGui.createGui(50,50,50,50,VideoRendererGui.ScalingType.SCALE_ASPECT_FILL,true);
            // ?????????????????? ??????.
            localVideoTrack.addRenderer(renderer);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    // ????????? ????????? ?????? ?????? , ?????? ????????? ?????? ( Connect ?????? onclick )
    public void CreateTurn(View button){
        if (peerConnection != null)
            return;
        MediaConstraints sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveVideo", "true"));
        sdpConstraints.optional.add(new MediaConstraints.KeyValuePair("internalSctpDataChannels", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
        sdpConstraints.optional.add(new MediaConstraints.KeyValuePair("RtpDataChannels", "true"));
        ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(new PeerConnection.IceServer("turn:3.34.179.97:3478","qtg_acc","1234qwer1919"));
        //iceServers.add(new PeerConnection.IceServer("turn:15.164.229.255:3478","qtg_acc","1234qwer1919"));
        //iceServers.add(new PeerConnection.IceServer( "turn:18.220.11.64:3478","qtg_acc","1234qwer1919"));
        peerConnection = peerConnectionFactory.createPeerConnection(
                iceServers,
                sdpConstraints,
                peerConnectionObserver);
        peerConnection.addStream(localMediaStream);
        try {
            socket = IO.socket(SIGNALING_URI);
            // ?????? ????????????
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    String socketID = socket.id();
                    Log.d("Test", "CreateTurn: "+SIGNALING_URI);
                    Log.d("SOCKET", "init: "+ socketID);
                    Log.d("SOCKET", "Connection success : " + socket.connected());
                    socket.emit("platform", roomname, Myplatform);
                }
            });
            //?????????
            socket.emit("roomjoin", roomname);

            socket.on(CREATEOFFER, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    createOffer = true;
                    Log.d("CreateOffer" , String.valueOf(createOffer));
                    MediaConstraints mediaConstraints = new MediaConstraints();
                    mediaConstraints.optional.add(new MediaConstraints.KeyValuePair("internalSctpDataChannels", "true"));
                    peerConnection.createOffer(sdpObserver, mediaConstraints);
                }
            }).on("platform", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    String obj = args[0].toString();

                    platform = obj;
                    Log.d("platform",platform);
                }
            }).on(OFFER, new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    try {
                        JSONObject obj = (JSONObject) args[0];
                        String type = obj.optString("type");
                        Log.d("offer" , String.valueOf(createOffer));
                        Log.d("offer" , type);
                        String sdpset = obj.optString("sdp");
                        Log.d("answer" , type);
                        Log.d("answer" , sdpset);
                        SessionDescription sdp = new SessionDescription(SessionDescription.Type.OFFER,
                                obj.getString(SDP));
                        Log.d("sessiondescription",sdp.description);
                        peerConnection.setRemoteDescription(sdpObserver, sdp);

                        if(platform.equals(web) == true){
                            Log.d("createanswer" , "???????????? ??????????????????");
                            peerConnection.createAnswer(new SdpObserver() {
                                @Override
                                public void onCreateSuccess(SessionDescription sessionDescription) {
                                    peerConnection.setLocalDescription(sdpObserver, sessionDescription);
                                    try {
                                        JSONObject obj = new JSONObject();
                                        String type = sessionDescription.type.toString().toLowerCase();
                                        obj.put("type", type);
                                        obj.put(SDP, sessionDescription.description);

                                            Log.d("Emit" , "answer");
                                            socket.emit(ANSWER, roomname,obj);
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onSetSuccess() {

                                }

                                @Override
                                public void onCreateFailure(String s) {
                                    Log.d("create Failed" , s);
                                }

                                @Override
                                public void onSetFailure(String s) {

                                }
                            }, new MediaConstraints());
                        }else{
                            peerConnection.createAnswer(sdpObserver, new MediaConstraints());
                        }
                        Log.d("createanswer" , "???????????? ??????????????????");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }).on(ANSWER, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        JSONObject obj = (JSONObject) args[0];
                        String type = obj.optString("type");
                        Log.d("answer class" ,"?????? ????????????");

                        SessionDescription sdp = new SessionDescription(SessionDescription.Type.fromCanonicalForm(type),
                                obj.getString(SDP));
                        peerConnection.setRemoteDescription(sdpObserver, sdp);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }).on(CANDIDATE, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        JSONObject obj = (JSONObject) args[0];
                        String tf = String.valueOf(platform.equals(web));
                        Log.d("plat", tf);
                        Log.d("getCandidate" , obj.getString("candidate"));
                        String[] sp = obj.getString("candidate").split("ufrag");
                        Log.d("getCandidate" , sp[0]);
                        Log.d("getCandidate",obj.getString(SDP_MID));
                        Log.d("getCandidate",obj.getString(SDP_M_LINE_INDEX));
                        if(platform.equals(web) == true){
                            Log.d("plat","web" );
                            peerConnection.addIceCandidate(new IceCandidate(obj.getString(SDP_MID),
                                    obj.getInt(SDP_M_LINE_INDEX),
                                    sp[0]));
                        }else{
                            Log.d("plat","app" );
                            peerConnection.addIceCandidate(new IceCandidate(obj.getString(SDP_MID),
                                    obj.getInt(SDP_M_LINE_INDEX),
                                    obj.getString(SDP)));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    // Offer < - > response ??????????????? SDP??? ?????????.
    SdpObserver sdpObserver = new SdpObserver() {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            peerConnection.setLocalDescription(sdpObserver, sessionDescription);
            try {
                JSONObject obj = new JSONObject();
                String type = sessionDescription.type.toString().toLowerCase();
                obj.put("type", type);
                obj.put(SDP, sessionDescription.description);
                Log.d("type sdp" , sessionDescription.type.toString());
                //obj.put(ROOMNAME , roomname);
                if (createOffer) {
                    Log.d("Emit" , "offer ");
                    socket.emit(OFFER, roomname, obj);
                } else {
                    Log.d("Emit" , "answer");
                    socket.emit(ANSWER, roomname,obj);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSetSuccess() {
            Log.d("SetSuccess" , "??????");
        }

        @Override
        public void onCreateFailure(String s) {
            Log.d("create Failed" , s);
        }

        @Override
        public void onSetFailure(String s) {
            Log.d("SetFailed" , s);
        }
    };

    PeerConnection.Observer peerConnectionObserver = new PeerConnection.Observer() {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.d("RTCAPP", "onSignalingChange:" + signalingState.toString());
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.d("RTCAPP", "onIceConnectionChange:" + iceConnectionState.toString());
        }


        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            Log.d("RTCAPP", "onIceGatheringChange:" + iceGatheringState.toString());
        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            try {
                JSONObject obj = new JSONObject();
                Log.d("sdpmid", iceCandidate.sdpMid);
                /*if(platform.equals(web)== true){
                    if(iceCandidate.sdpMid.equals("audio")){
                        obj.put(SDP_MID, "0");
                    }else{
                        obj.put(SDP_MID, "1");
                    }
                }else {*/
                    obj.put(SDP_MID, iceCandidate.sdpMid);
                //}
                obj.put(SDP_M_LINE_INDEX, iceCandidate.sdpMLineIndex);
                obj.put(SDP, iceCandidate.sdp);
                socket.emit(CANDIDATE, roomname,obj);
                Log.d("send Candidate" , obj.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            mediaStream.videoTracks.getFirst().addRenderer(otherPeerRenderer);
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            mediaStream.videoTracks.getLast().removeRenderer(otherPeerRenderer);
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {

        }

        @Override
        public void onRenegotiationNeeded() {

        }
    };


}