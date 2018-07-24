package cn.orangeiot;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-07-17
 */
public class SerializableMethod implements Serializable {
    private static final long serialVersionUID = 6631604036553063657L;
    private Method method;

    public SerializableMethod(Method method) {
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeObject(method.getDeclaringClass());
        out.writeUTF(method.getName());
        out.writeObject(method.getParameterTypes());
    }


    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        Class<?> declaringClass = (Class<?>) in.readObject();
        String methodName = in.readUTF();
        Class<?>[] parameterTypes = (Class<?>[]) in.readObject();
        try {
            method = declaringClass.getMethod(methodName, parameterTypes);
        } catch (Exception e) {
            throw new IOException(String.format("Error occurred resolving deserialized method '%s.%s'", declaringClass.getSimpleName(), methodName), e);
        }
    }
}
