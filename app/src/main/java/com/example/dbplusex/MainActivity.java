package com.example.dbplusex;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

//1.파일을 DB로 변환
//2.변환 후 테이블 설정
//3. 안드로이드 스튜디오에서 assets폴더에 복사
//4. 프로그램으로 assets 폴더에 있는 DB를  /data/data/패키지이름/dataabases폴더 에 복사
//5.   /data/data/패키지이름/dataabases 에있는 DB 활용

    SQLiteDatabase sqlDB;
    Spinner spinnerAdd,spinnerName;
    LinearLayout linear1;
    TextView tv1;
    Button btnMap;
    String fileName = "/data/data/com.example.dbplusex/databases/patHDB.db";


    ArrayList<String> cData = new ArrayList<String>();
    ArrayList<String> hNameData = new ArrayList<String>();  //동물 이름을 담아 둘 동적 배열
//    String result;  //결과를 담을 변수 생성



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        spinnerAdd = (Spinner)findViewById(R.id.spinnerAdd);
        spinnerName = (Spinner)findViewById(R.id.spinnerName);
        btnMap = (Button)findViewById(R.id.btnMap);
        linear1 = (LinearLayout)findViewById(R.id.linear1);
        tv1 = (TextView)findViewById(R.id.tv1);





        boolean bResult = isCheckDB(this);
        try {
        if(bResult==false) {
            copyDB(this);
        }
        }catch (Exception e) {//파일을 못 읽어 올때 처리
           showToast("파일을 읽을 수 없습니다.");
        }

        sqlDB=SQLiteDatabase.openDatabase(fileName,null,SQLiteDatabase.OPEN_READONLY);  //일기장에서는 db를 불러들여왔지만 지금은 db를 생성하는 것이 아니고 카피해서 사용한다.
                                        //(db경로,factory값,읽기전용);


        Cursor cursor1;
        cursor1 = sqlDB.rawQuery("SELECT distinct(CityName) FROM patHTBL ;",null);   //distinct(sigun) 중복을 제거해준다.

        while (cursor1.moveToNext()) {
            cData.add(cursor1.getString(0));  //가져온 필드에 따라서 순서가 정해지는데, 이 경우는 필드가 1개 이므로 0
        }
        cursor1.close();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_spinner_item,cData);
        spinnerAdd.setAdapter(adapter);
        spinnerAdd.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {  //첫번째 스피너에 선택이 된다면 실행되는 메소드
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                hNameData.clear();  //동적배열을 초기화 하는 명령어
                Cursor cursor2;
                cursor2 = sqlDB.rawQuery("SELECT HospitalName FROM patHTBL WHERE CityName = '"+spinnerAdd.getSelectedItem().toString()+ "';" ,null);  //spinnerAdd.getSelectedItem().toString() 첫번째 스피너에서 고른 내용
                while (cursor2.moveToNext()) {
                    hNameData.add(cursor2.getString(0));  //여기도 0번째인 이유 : 여기도 마찬가지로 내가 가져온 필드가 1개이므로 0이다.  3개의 필드를 가져오고 내가 바꿔야되는 필드가 3번째인 경우 2가 들어간다.(0,1,2)
                }

                ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_spinner_item,hNameData);   // MainActivity.this 이름을 명확히 해줘야 할 때가 있다.
                spinnerName.setAdapter(adapter1);
                cursor2.close();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinnerName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {  //두번째 스피너에 선택이 된다면, 실행되는 메소드
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor3 ;
                cursor3 = sqlDB.rawQuery("SELECT * FROM patHTBL WHERE CityName = '" + spinnerAdd.getSelectedItem().toString()+ "' AND HospitalName = '"+spinnerName.getSelectedItem().toString()+ "';",null );
                // ★★★동물병원이 중복되있는 이름이 있기 때문에, WHERE 조건에, 먼저 첫번째 스피너(필드)에 있는 것을 먼저 확인 한 후에 AND 연산자로 더한 조건을 만들어준다.
                String result;  //결과를 담을 변수 생성
                cursor3.moveToFirst();
                result = "소재지 : " + cursor3.getString(0) + "\n";
                result+= "병원이름 : " + cursor3.getString(1) + "\n";
                if(cursor3.getString(3).equals("정상")) {
                    result+= "개업일 : " + cursor3.getString(2) + "\n";
                }else  {
                    result+= "개업일 : " + cursor3.getString(2) + "(" + cursor3.getString(3)+ " - "+ cursor3.getString(4) + " ) " + "\n";
                }
                result+= "전화번호 : " + cursor3.getString(5) + "\n";
                result+= "우편번호 : " + cursor3.getString(6) + "\n";
                result+= "주소 : " + cursor3.getString(7) + "\n";
                tv1.setText(result);
                cursor3.close();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

    }



    public boolean isCheckDB(Context context) { // db가 있는지 없는지 확인해주는 메소드 + db가 갱신(늘어나거나 줄어드는 경우)되면 db를 복사 해주는 역할도 한다.
        String filePath = "/data/data/com.example.dbplusex/databases/patHDB.db";
        File file = new File(filePath);
        long newdb_size = 0;
        long olddb_size = file.length();  //파일의 크기 값이 들어간다.
        AssetManager manager = context.getAssets();  //asset 폴더에 접근시킬 수 있는 클래스
        try {
            InputStream is = manager.open("patHDB.db");   //파일을 읽어오는 명령어 중에 앱 안에 읽어오는 명령어는 InputStream
            newdb_size=is.available();

        }catch (IOException e) {  //파일을 못 읽어 올때 처리
            showToast("파일을 읽을 수 없습니다.");
        }
        if(file.exists()) {  //파일이 존재한다면
            if(newdb_size!=olddb_size) { //폴더안에 있는  patHDB.db와 assets폴더 안에 있는 patHDB.db 크기가 다른 경우
                return false;
            }else {
                return true;
            }
        }return  false;
    }
    public void copyDB(Context context) {  // assets폴더안의 db파일을      /data/data/패키지이름/dataabases폴더 생성 후 db파일을 복사해주는 역활을 하는 메소드
        AssetManager manager = context.getAssets();
        String folderPath = "/data/data/com.example.dbplusex/databases";
        String filePath = "/data/data/com.example.dbplusex/databases/patHDB.db";
        File folder = new File(folderPath);
        File file = new File(filePath);
        FileOutputStream  fos = null;  //파일을 복사에서 보내야되기 때문에 FileOutputStream
        BufferedOutputStream bos = null;  //파일을 옮기는 역할을 하는 것이 Buffer 이다. BufferedOutputStream
        try {
            InputStream is = manager.open("patHDB.db");
            BufferedInputStream bis = new BufferedInputStream(is);
            if(! folder.exists()) { //처음에 폴더가 존재하지 않는다면
                folder.mkdir(); //폴더 생성
            }
            if(file.exists()) { //파일이 존재한다면,   //폴더안에 있는  patHDB.db와 assets폴더 안에 있는 patHDB.db 크기가 다른 경우
                file.delete();  //기존 파일 삭제
                file.createNewFile(); //새로운 파일 생성해준다.
            }

            //파일처리 구문
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            int read = -1;
            byte buffer[] = new byte[1024];   //파일의 크기가 1mb를 넘어가면 안되기 때문
            while ((read=bis.read(buffer,0,1024)) != -1) {  //bis에는 현재 patHDB.db 정보가 있음. read 담아주는 역할
                bos.write(buffer,0,read); //bos에도 현재 patHDB.db 정보가 다 담겨있다.
            }
            bos.flush(); //정리  -->비워놔야 다음에 또 사용이 가능하다.
            bos.close(); //닫기
            fos.close(); //닫기
            bis.close(); //닫기
            is.close(); //닫기
        }catch (IOException e) { //파일을 복사 할 수 없을 때 처리
            showToast("파일을 복사 할 수 없습니다.");
        }
    }
    void showToast (String msg) {
        Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_SHORT).show();
    }
}
