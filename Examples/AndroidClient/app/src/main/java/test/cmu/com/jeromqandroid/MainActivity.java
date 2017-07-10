package test.cmu.com.jeromqandroid;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import common.Utils;
import communication.ClientCommController;
import communication.SessionMessage;
import edu.cmu.inmind.multiuser.common.model.SaraOutput;

public class MainActivity extends AppCompatActivity {
    public static final boolean VERBOSE = false;
    private static final String CONNECTED = "CONNECTED";
    private static final String SENDING = "SENDING";
    private static final String STOPPED = "STOPPED";
    private static final String START = "START";

    public static final int NUM_ITER = 1;
    private static final int NUM_CLIENTS = 1;
    private static final boolean shouldCheckTime = false;

    private EditText asrInput;
    private EditText ipAddress;
    private TextView response;
    private ScrollView scrollView;
    private Button connectBtn;
    private String state;
    private ClientCommController[] clientCommControllers;
    private long[] average;
    private int[] receivedMessages;
    private StringBuffer responseStr;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        asrInput = (EditText) findViewById( R.id.asrInput );
        ipAddress = (EditText) findViewById( R.id.ipInput );
        response = (TextView) findViewById( R.id.response );
        scrollView = (ScrollView) findViewById(R.id.scrollerResponse);
        scrollView.fullScroll(View.FOCUS_DOWN);
        connectBtn = (Button) findViewById(R.id.connectBtn);
        connectBtn.setFocusable(true);
        connectBtn.setFocusableInTouchMode(true);
        connectBtn.requestFocus();
        clientCommControllers = new ClientCommController[NUM_CLIENTS];
        average = new long[NUM_CLIENTS];
        receivedMessages = new int[NUM_CLIENTS];
        responseStr = new StringBuffer();
        state = START;
    }

    private void initialize() {
        for(int i = 0; i < NUM_CLIENTS; i++ ){
            clientCommControllers[i] = new ClientCommController(ipAddress.getText().toString(),
                    "-" + i, this);
        }
    }


    public void stop(View view) {
        if( state.equals( CONNECTED ) || state.equals( SENDING ) ) {
            state = STOPPED;
            for (int i = 0; i < clientCommControllers.length; i++) {
                clientCommControllers[i].stopClient();
                clientCommControllers[i] = null;
            }
        }
    }

    public void connect(View view) {
        if( state.equals( STOPPED ) || state.equals( START ) ) {
            state = CONNECTED;
            initialize();
            for (final ClientCommController commController : clientCommControllers) {
                new Thread() {
                    @Override
                    public void run() {
                        commController.start();
                    }
                }.start();
                // if we are running a lot of clients (i.e., 100) we need a delay between initializations
                // of 500ms, otherwise you can use a lower delay as 100ms.
                Utils.sleep(1000);
            }
        }
    }

    public void send(View view) {
        if( state.equals( CONNECTED) || state.equals( SENDING )) {
            state = SENDING;
            for (int i = 0; i < NUM_ITER; i++) {
                for (ClientCommController commController : clientCommControllers) {
                    if( shouldCheckTime ) {
                        commController.send(String.format("%s-%d:%d", "", i,
                                System.currentTimeMillis()));
                    }else{
                        commController.send( asrInput.getText().toString() );
                    }
                }
                // we need a delay in order to not collapse the communication controller
                Utils.sleep(30);
            }
        }
    }

    public void processOutput(String resp, String clientName) throws Exception{
        int idx = Integer.valueOf( clientName.substring( clientName.indexOf("-") + 1 ) );
        receivedMessages[idx]++;
        SessionMessage sessionMessage = Utils.fromJson(resp, SessionMessage.class);
        SaraOutput saraOutput = Utils.fromJson(sessionMessage.getPayload(), SaraOutput.class);
        String userIntent = saraOutput.getUserIntent().getUserIntent();
        if( shouldCheckTime ) {
            long totalTime = (System.currentTimeMillis() -
                    Long.valueOf(userIntent.substring(userIntent.indexOf(":") + 1, userIntent.length())));
            average[idx] += totalTime;
        }else{
            responseStr.append( Utils.toJson( saraOutput ) + "\n" );
        }
        print();
    }

    private void print(){
        for(int i = 0; i < NUM_CLIENTS; i ++ ){
            if( receivedMessages[i] > 0 ) {
                if( shouldCheckTime ) {
                    responseStr.append(String.format("Average time: %d \n",
                            (average[i] / receivedMessages[i])));
                }
                response.setText(responseStr.toString());
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        }
    }
}
