
package mx.itesm.edu.earthone.chatapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class ChatActivity extends Activity {
    private Button button;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private ChildEventListener childEventListener;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener stateListener;

    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;

    private final int LOGIN = 123;

    private Button logout;

    private EditText editText;
    private ListView listView;
    private ChatAdapter adapter;
    private String name;
    private ImageButton imageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference().child("messages");

        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference().child("pics");

        firebaseAuth = FirebaseAuth.getInstance();

        listView = (ListView) findViewById(R.id.listView);
        adapter = new ChatAdapter(this,R.layout.chat_layout, new ArrayList<ChatPojo>());
        listView.setAdapter(adapter);

        button = (Button) findViewById(R.id.button2);
        editText = (EditText) findViewById(R.id.editText2);
        logout = (Button) findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AuthUI.getInstance().signOut(getApplicationContext());
            }
        });

        imageButton = (ImageButton) findViewById(R.id.imageButton);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Select a PIC!"), 3007);
            }
        });



        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){


                String message = editText.getText().toString();
                ChatPojo chatPojo =
                        new ChatPojo(name,null,message);
                databaseReference.push().setValue(chatPojo);
                editText.setText("");
            }
        });

        stateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if(firebaseUser != null){
                    Toast.makeText(getApplicationContext(),
                            "Welcome",Toast.LENGTH_LONG).show();
                    loadChats();
                    name = firebaseUser.getDisplayName();
                }else{
                    clean();
                    startActivityForResult(
                            AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(Arrays.asList(
                                    new AuthUI.IdpConfig.EmailBuilder().build(), new AuthUI.IdpConfig.GoogleBuilder().build()
                            )
                            ).build(), LOGIN
                    );
                }

            }
        };


    }
    private void clean(){
        name = "";
        adapter.clear();
        if(childEventListener != null){
            databaseReference.removeEventListener(childEventListener);
            childEventListener = null;
        }
    }

    private void loadChats(){
        if(childEventListener == null) {
            childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    ChatPojo chatPojo = dataSnapshot.getValue(ChatPojo.class);
                    adapter.add(chatPojo);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };
            databaseReference.addChildEventListener(childEventListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        firebaseAuth.removeAuthStateListener(stateListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(firebaseAuth != null){
            firebaseAuth.addAuthStateListener(stateListener);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 3007 && resultCode == RESULT_OK){
            Uri pic = data.getData();
            StorageReference str = storageReference.child(pic.getLastPathSegment());
            str.putFile(pic).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri uploaded = taskSnapshot.getDownloadUrl();
                    ChatPojo chatPojo = new ChatPojo(name,uploaded.toString(),null);
                    databaseReference.push().setValue(chatPojo);
                }
            });

        }

    }
}
