package com.luckyend.flatmapper.functions;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import lombok.NoArgsConstructor;

/**
 * Default function map factory
 * 
 * @author Ariel Carrera
 *
 */
@NoArgsConstructor
public class DefaultFunctionMapFactory implements FunctionMapFactory {
	
	private static FunctionMapFactory INSTANCE = null;
	
	private static char[] alphabet = "ABCDEFGHYJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
	private static char[] alphanumeric = "ABCDEFGHYJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890".toCharArray();
	
	public static FunctionMapFactory getDefaultInstance() {
        if (INSTANCE == null){
        	synchronized(DefaultFunctionMapFactory.class){
                if(INSTANCE == null) // check again within synchronized block to guard for race condition
                    INSTANCE = new DefaultFunctionMapFactory();
            }
        }
        return INSTANCE;
    }
	
	protected Map<String, Function<String[], Object>> functionMap;
	
	/* (non-Javadoc)
	 * @see com.luckyend.flatmapper.function.FunctionMapFactory#getFunctionMap()
	 */
	@Override
	public Map<String, Function<String[], Object>> getFunctionMap(){
		if (functionMap == null) {
			functionMap = new HashMap<>();
			functionMap.put("RANDOM_INT", this::randomInt);
			functionMap.put("RANDOM_INTEGER", this::randomInt);
			functionMap.put("RANDOM_LONG", this::randomLong);
			functionMap.put("RANDOM_DOUBLE", this::randomDouble);
			functionMap.put("RANDOM_FLOAT", this::randomFloat);
			functionMap.put("RANDOM_BOOLEAN", this::randomBoolean);
			functionMap.put("RANDOM_STRING", this::randomString);
			functionMap.put("RANDOM", this::random);
		}
		
		return functionMap;
	}
	
	
	public Integer randomInt(String[] param){
		int from = Integer.MIN_VALUE;
		int to = Integer.MAX_VALUE;
		
		if (param != null && param.length > 0){
			from = Integer.parseInt(param[0]);
			if (param.length >= 2){
				to = Integer.parseInt(param[1]);
				return ThreadLocalRandom.current().nextInt(from, to);
			}
			return ThreadLocalRandom.current().nextInt(from);
		}
		
		return ThreadLocalRandom.current().nextInt();
	}
	
	public Float randomFloat(String[] param){
		float from,to;
		
		if (param != null && param.length > 0){
			from = Float.parseFloat(param[0]);
			if (param.length >= 2){
				to = Float.parseFloat(param[1]);
				return from + ThreadLocalRandom.current().nextFloat() * (to - from);
			} else {
				return from + ThreadLocalRandom.current().nextFloat();
			}
		} 
		return ThreadLocalRandom.current().nextFloat();
	}
	
	public Long randomLong(String[] param){
		long from = Long.MIN_VALUE;
		long to = Long.MAX_VALUE;
		
		if (param != null && param.length > 0){
			from = Long.parseLong(param[0]);
			if (param.length >= 2){
				to = Long.parseLong(param[1]);
				return ThreadLocalRandom.current().nextLong(from, to);
			}
			return ThreadLocalRandom.current().nextLong(from);
		}
		
		return ThreadLocalRandom.current().nextLong();
	}
	
	public Double randomDouble(String[] param){
		double from = Double.MIN_VALUE;
		double to = Double.MAX_VALUE;
		
		if (param != null && param.length > 0){
			from = Double.parseDouble(param[0]);
			if (param.length >= 2){
				to = Double.parseDouble(param[1]);
				return ThreadLocalRandom.current().nextDouble(from, to);
			}
			return ThreadLocalRandom.current().nextDouble(from);
		}
		
		return ThreadLocalRandom.current().nextDouble();
	}
	
	public Boolean randomBoolean(String[] param){
		return ThreadLocalRandom.current().nextBoolean();
	}
	
	public String random(String[] param){
		if (param != null && param.length > 0){
			return param[ThreadLocalRandom.current().nextInt(param.length)];
		}
		return "";
	}
	
	/**
	 * Retorna un texto aleatorio (alfanumerico por defecto). Por defecto crea aleatorios de 8 caracteres
	 * 
	 * @param param puede recibir como primer parametro la longitud y como segundo parametro true o false si debe utilizar solo Letras (sin numeros)
	 * @return
	 */
	public String randomString(String[] param){
		int length = 8; 
		boolean soloLetras = false;
		if (param != null && param.length > 0){
			length = Integer.parseInt(param[0]);
			if (length <= 0) throw new IllegalArgumentException("El valor indicado debe ser mayor a 0");
			if (param.length > 1){
				soloLetras = Boolean.parseBoolean(param[1]);
			}
		}
		char[] array;
		if (soloLetras){
			array = alphabet;
		} else {
			array = alphanumeric;
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
		    char c = array[ThreadLocalRandom.current().nextInt(array.length)];
		    sb.append(c);
		}
		
		return sb.toString();
	}

}