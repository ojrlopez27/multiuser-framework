package edu.cmu.inmind.multiuser.common;

import com.google.gson.Gson;
import com.rits.cloning.Cloner;
import edu.cmu.inmind.multiuser.controller.blackboard.BlackboardSubscription;
import edu.cmu.inmind.multiuser.controller.exceptions.ExceptionHandler;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Created by oscarr on 4/20/16.
 */
public class Utils {
    private static final int DEFAULT_TIME_SPAN = 1;  //1 year
    public static final String ISO_8601_24H_FULL_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static Gson gson = new Gson();
    public static String log = "";
    //public static final String ISO_8601_24H_FULL_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    /**
     * Returns a date which is increased a x amount of field (Calendar.DAY_OF_MONTH, Calendar.MONTH, etc)
     * in relation to the current date and time.
     * @param field
     * @param amount
     * @return
     */
    public static Date getRelativeDate(int field, int amount) {
        return getRelativeDate(new Date(), field, amount);
    }

    public static Date getRelativeDate(Date date, int field, int amount){
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        if( amount > 0 ) {
            cal.add(field, amount);
        }else{
            if( field == Calendar.DAY_OF_YEAR ) {
                cal.add(field, DEFAULT_TIME_SPAN * 365); //1 year
            }else if( field == Calendar.MONTH ) {
                cal.add(field, DEFAULT_TIME_SPAN * 12); //1 years
            }else if( field == Calendar.YEAR ) {
                cal.add(field, DEFAULT_TIME_SPAN); //1 years
            }
        }
        return cal.getTime();
    }

    /**
     * If format is null then "yyyy/MM/dd" will be the default format
     * @param format
     * @return
     */
    public static String getDate(Date date, String format) {
        if( format == null ) format = "yyyy/MM/dd";
        return new SimpleDateFormat( format ).format(date);
    }

    public static Date getOnlyeDate(Date date) {
        if( date == null ) return null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int day = calendar.get( Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get( Calendar.YEAR);
        return getDate(year, month, day);
    }

    public static Date getDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set( Calendar.YEAR, year );
        cal.set( Calendar.MONTH, month );
        cal.set( Calendar.DAY_OF_MONTH, day );
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set( Calendar.MINUTE, 0);
        cal.set( Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static Date getDate(int year, int month, int day, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set( Calendar.YEAR, year );
        cal.set( Calendar.MONTH, month );
        cal.set( Calendar.DAY_OF_MONTH, day );
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set( Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static Date getTime(Date date, int hourOfDay, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set( Calendar.MINUTE, minute );
        cal.set(Calendar.SECOND, 0);
        return cal.getTime();
    }

    /**
     * It returns a full date (date + time)
     * @param date
     * @param time in format HH:MM
     * @return
     */
    public static Date getDateTime(Date date, String time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY,
                Integer.valueOf(time.substring(0, time.indexOf(":"))));
        calendar.set(Calendar.MINUTE,
                Integer.valueOf(time.substring(time.indexOf(":") + 1)));
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    /**
     * It returns a Date in yyyy/MM/dd format
     * @param miliseconds
     * @return
     */
    public static String formatDate( long miliseconds ){
        return new SimpleDateFormat("yyyy/MM/dd").format(new Date(miliseconds));
    }


    public static int getDateField( Date date, int field ) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime( date );
        return calendar.get(field);
    }


    public static boolean isDateInRange( long timeToEvaluate, long threshold, long timeReference ){
        long minRangeTime = timeToEvaluate - threshold/2;
        long maxRangeTime = timeToEvaluate + threshold/2;
        return timeReference >= minRangeTime && timeReference <= maxRangeTime;
    }


    public static Date getDate(String formattedDate){
        try {
            TimeZone timezone = TimeZone.getTimeZone("GMT" + formattedDate.substring( formattedDate.indexOf("+") ));
            SimpleDateFormat sdf = new SimpleDateFormat(ISO_8601_24H_FULL_FORMAT);
            sdf.setTimeZone( timezone );
            return sdf.parse(formattedDate);
        }catch (Throwable e){
            e.printStackTrace();
        }
        return null;
    }


    public static void sleep(long millis){
        try{
            Thread.yield();
            Thread.sleep(millis);
        }catch (Throwable e){
            e.printStackTrace();
        }
    }


    public static void toJson(Object object, String name){
        PrintWriter out = null;
        try {
            String json = gson.toJson(object);
             out = new PrintWriter(name + ".json");
            out.println(json);
            out.flush();
            out.close();
        }catch (Throwable e){
            e.printStackTrace();
        }finally {
            try {
                out.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static String toJson(Object object){
        try {
            return gson.toJson(object);
        }catch (Throwable e){
            e.printStackTrace();
        }
        return null;
    }

    public static <T> T fromJson(String stringRepresentation, Class<T> clazz){
        try {
            return gson.fromJson( trimDoubleQuotes(stringRepresentation), clazz);
        }catch (Throwable e){
            e.printStackTrace();
        }
        return null;
    }

    private static String trimDoubleQuotes(String stringRepresentation) throws Throwable{
        boolean trimmed = false;
        if( stringRepresentation.substring(0, 1).equals("\"") ){
            stringRepresentation = stringRepresentation.substring( 1 );
            trimmed = true;
        }
        if( stringRepresentation.substring( stringRepresentation.length() - 1).equals( "\"" ) ){
            stringRepresentation = stringRepresentation.substring( 0, stringRepresentation.length() - 1);
            trimmed = true;
        }
        return trimmed? stringRepresentation.replace("\\", "") : stringRepresentation;
    }


    public static String getDateString() throws Throwable{
        SimpleDateFormat format = new SimpleDateFormat("[yyyy-MM-dd-HH.mm.ss]");
        return format.format( new Date() );
    }

    public static void exchange(String[] conversationalStrategies, String behaviorName) throws Throwable{
        if( !conversationalStrategies[0].equals(behaviorName) ) {
            ArrayList<String> list = new ArrayList(Arrays.asList(conversationalStrategies));
            list.remove(behaviorName);
            list.add(0, behaviorName);
            for (int i = 0; i < list.size(); i++) {
                conversationalStrategies[i] = list.get(i);
            }
        }
    }

    public static <T> T createInstance(Class<T> clazz, Object... args) {
        Class[] parameterTypes = null;
        try {
            parameterTypes = new Class[args.length];
            for( int i = 0; i < args.length; i++ ){
                if( args[i] instanceof Class ){
                    parameterTypes[i] = (Class)args[i];
                    args[i] = null;
                }else {
                    parameterTypes[i] = args[i].getClass();
                }
            }
            Constructor constructor = clazz.getConstructor( parameterTypes );
            return (T)constructor.newInstance( args );
        }catch (Throwable e){
            try{
                for( int i = 0; i < args.length; i++ ){
                    if( args[i] != null && !(args[i] instanceof Class) ){
                        Class argClass = args[i].getClass().getSuperclass();
                        if( argClass != Object.class && argClass != Class.class ) {
                            parameterTypes[i] = argClass;
                        }
                    }
                }
                Constructor constructor = clazz.getConstructor( parameterTypes );
                return (T)constructor.newInstance( args );
            }catch(Throwable e1){
                e1.printStackTrace();
            }
        }
        return null;
    }


    private static Properties properties;
    private static InputStream input = null;

    public static String getProperty(String key){
        String value = "";
        try {
            if( properties == null ) {
                input = new FileInputStream("config.properties");
                properties = new Properties();
                properties.load(input);
            }
            value = properties.getProperty( key );
        } catch (Throwable ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return value;
    }

    public static Properties loadProperties(String pathName){
        Properties prop = null;
        FileInputStream inputStream = null;
        try {
            prop = new Properties();
            inputStream = new FileInputStream(pathName);
            prop.load( new FileInputStream(pathName) );
        }catch (Exception e){
            e.printStackTrace();
        }finally{
            try {
                inputStream.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return prop;
    }

    public static void checkAssert(boolean expression) {
        if( !expression ){
            ExceptionHandler.handle( new Exception("Assertion error.") );
        }
    }

    /**********************************************************************************************/
    /************************************** CLONE *************************************************/
    /**********************************************************************************************/

    private static Cloner cloner = new Cloner();

    public static <T> T clone( T object ){
        return cloner.deepClone(object);
    }

    public static <T extends List> T cloneList( T list ){
        return cloner.deepClone(list);
    }

    public static ArrayList cloneArray( ArrayList list ) throws Throwable{
        ArrayList result = new ArrayList(list.size());
        for( Object element : list ){
            result.add( cloner.deepClone(element) );
        }
        return result;
    }

    public static Class getClass(Object caller) {
        Class clazz = null;
        String className;
        try {
            clazz = caller.getClass();
            className = clazz.getName();
            if (className.contains("$$Enhancer")) {
                clazz = Class.forName(className.substring(0, className.indexOf("$$Enhancer")));
            }
        }catch (Throwable e) {
            ExceptionHandler.handle(e);
        }
        return clazz;
    }

    /**
     * Changes the annotation value for the given key of the given annotation to newValue and returns
     * the previous value.
     */
    @SuppressWarnings("unchecked")
    public static Object addOrChangeAnnotation(Annotation annotation, String key, Object newValue)
            throws Throwable{
        Object handler = Proxy.getInvocationHandler(annotation);
        Field f;
        try {
            f = handler.getClass().getDeclaredField("memberValues");
        } catch (NoSuchFieldException | SecurityException e) {
            throw new IllegalStateException(e);
        }
        f.setAccessible(true);
        Map<String, Object> memberValues;
        try {
            memberValues = (Map<String, Object>) f.get(handler);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        Object oldValue = memberValues.get(key);
        if (oldValue == null || oldValue.getClass() != newValue.getClass()) {
            throw new IllegalArgumentException();
        }
        memberValues.put(key, newValue);
        return oldValue;
    }

    public static void toJsonFile(Object obj, String directory, String fileName) {
        PrintWriter writer = null;
        if( obj != null ) {
            try {
                File dir = new File(directory);
                if( !dir.isDirectory() ){
                    dir.mkdir();
                }
                File file = new File( directory, fileName);
                writer = new PrintWriter(file, "UTF-8");
                writer.print( gson.toJson( obj ) );
            } catch (Throwable e) {
                e.printStackTrace();
            }finally {
                if( writer != null ){
                    writer.flush();
                    writer.close();
                }
            }
        }
    }

    public static <T> T fromJsonFile(String fileName, Class<T> clazz) throws Exception{
        if( fileName == null ) return null;
        Scanner scanner = null;
        try {
            File file = new File( fileName );
            if( file.exists() ) {
                scanner = new Scanner(file, "UTF-8");
                String text = scanner.useDelimiter("\\A").next();
                return fromJson(text, clazz);
            }else{
                throw new FileNotFoundException();
            }
        } catch (FileNotFoundException e) {
            throw e;
        }finally {
            if( scanner != null ) {
                scanner.close();
            }
        }
    }



    public static void renameFile(String pathAndNameOriginalFile, String nameFinalFile) throws Throwable{
        try {
            Path yourFile = Paths.get(pathAndNameOriginalFile);
            Files.move(yourFile, yourFile.resolveSibling(nameFinalFile), REPLACE_EXISTING);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static <T extends Annotation> T getAnnotation(Class<?> clazz, Class<T> annotationType)
            throws Throwable{
        T result;
        boolean isAnnotationPresent = clazz.isAnnotationPresent(annotationType);
        if(!isAnnotationPresent){
            Class<?> superClazz = clazz.getSuperclass();
            if(superClazz!=null){
                return getAnnotation(superClazz, annotationType);
            }
            else{
                return null;
            }
        }else{
            result = clazz.getAnnotation(annotationType);
            return result;
        }
    }



    public static boolean isURLvalid(String address){
        //TODO: we need to replace this with a proper reg exp
        if( address.startsWith("tcp://") ){
            String[] ip = address.substring( 6 ).split("\\." );
            for(String segment : ip ){
                if( segment.contains(":") ){
                    segment = segment.split(":")[0];
                }
                int seg = Integer.valueOf(segment);
                if( seg < 0 || seg > 255 ){
                    return false;
                }
            }
            return true;
        }
        return false;
    }


    /**
     * https://praveer09.github.io/technology/2016/02/29/rxjava-part-3-multithreading/
     * This method executes a callable in a new thread
     *
     * @param callable
     * @param mapper
     * @param consumer
     */
    public static void execObsParallel(Callable callable, Function mapper, Consumer consumer){
        io.reactivex.Observable.fromCallable(callable)
                .map(mapper)
                .observeOn(Schedulers.newThread())      // subscriber on different thread
                .subscribe(consumer);
    }

    public static void execObsParallel(Consumer consumer){
        execObsParallel( () -> "", s -> "", consumer );
    }

    /**
     * This method executes an observabe sequentially (i.e., it doesn't start the next request (consumer) until
     * the current one finishes)
     * @param consumer
     */
    public static void execObsSequential(Consumer consumer){
        io.reactivex.Observable.fromCallable(() -> consumer )
                .observeOn(Schedulers.single())
                .subscribe( consumer );
    }

}
