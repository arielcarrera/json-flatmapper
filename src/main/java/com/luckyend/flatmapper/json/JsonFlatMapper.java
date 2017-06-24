package com.luckyend.flatmapper.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.luckyend.flatmapper.FlatMapper;
import com.luckyend.flatmapper.functions.DefaultFunctionMapFactory;
import com.luckyend.flatmapper.functions.FunctionMapFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonFlatMapper implements FlatMapper<String> {
	
	private ObjectMapper mapper;
	
	private Map<String, Function<String[], Object>> functionMap;
    
    public JsonFlatMapper() {
		super();
		initObjectMapper();
	}
    
    public JsonFlatMapper(ObjectMapper omapper) {
		super();
		mapper = omapper;
		log.warn("Default functionMapFactory is instantiated.");
		functionMap = DefaultFunctionMapFactory.getDefaultInstance().getFunctionMap();
	}
    
    public JsonFlatMapper(FunctionMapFactory<Object> functionMapFactory) {
		super();
		log.warn("Default objetMapper is instantiated.");
		initObjectMapper();
		if (functionMapFactory == null) {
			log.warn("FunctionMapFactory is null. Default functionMapFactory is instantiated.");
			functionMap = DefaultFunctionMapFactory.getDefaultInstance().getFunctionMap();
		} else {			
			functionMap = functionMapFactory.getFunctionMap();
		}
	}
    
    public JsonFlatMapper(ObjectMapper omapper, FunctionMapFactory<Object> functionMapFactory) {
		super();
		if (omapper == null) {
			log.warn("ObjectMapper is null. Default objetMapper is instantiated.");
			initObjectMapper();
		} else {
			mapper = omapper;
		}
		if (functionMap == null) {
			log.warn("FunctionMapFactory is null. Default functionMapFactory is instantiated.");
			functionMap = DefaultFunctionMapFactory.getDefaultInstance().getFunctionMap();
		} else {			
			functionMap = functionMapFactory.getFunctionMap();
		}
	}
	
    protected void initObjectMapper() {
		mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        	.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        mapper.setSerializationInclusion(Include.NON_NULL);
	}
	
	
	/* (non-Javadoc)
	 * @see com.luckyend.flatmapper.json.FlatMapper#convertFromFlatMap(java.util.Map)
	 */
	@Override
	public String convertFromFlatMap(Map<String, Object> flatMapFrom) {
        return convertFromFlatMap(flatMapFrom, true);
    }
	
	/* (non-Javadoc)
	 * @see com.luckyend.flatmapper.json.FlatMapper#convertFromFlatMap(java.util.Map, boolean)
	 */
	@Override
	public String convertFromFlatMap(Map<String, Object> flatMapFrom, boolean createCopy) {
		Map<String, Object> from;
		if (createCopy) {
			from = new HashMap<>(flatMapFrom);
			from.values().removeIf(Objects::isNull);
		} else {
			from = flatMapFrom;
		}
		//Si no quedan items en convertFromCopy por que todos eran nulos o llego vacio, se retorna nulo
		if (from.size() == 0) return null;
		
		Set<String> keys = from.keySet();

        if (from != null) {
        	ObjectNode json = mapper.createObjectNode();

        	//filtro solo los atributos del primer nivel que sean de tipos simples
        	List<String> simplesPrimerNivel = creaObjetosSimples(from, json, keys, "");
        	
        	//filtro los atributos del primer nivel que sean de tipo complejo (objetos)
        	List<String> objectosPrimerNivel = creaObjetosComplejos(from, keys, json, "");

        	try {
    			return mapper.writeValueAsString(json);
    		} catch (Exception e) {
    			log.error("Error converting map to json",e);
    		}
    		
        }
        return null;
    }

	@Override
	public Map<String, Object> convertToFlatMap(String jsonFrom) {
		Map<String, Object> map = new HashMap<String, Object>();

		// convert JSON string to flatMap
		try {
			map = mapper.readValue(jsonFrom, new TypeReference<Map<String, String>>(){});
		} catch (IOException e) {
			log.error("Error converting json to map",e);
		}
        return map;
    }
	
	
	private List<String> creaObjetosComplejos(Map<String, Object> convertFrom, Set<String> keys, ObjectNode padre, String prefix) {
		List<String> objPrimerNivel = keys.stream().filter(key -> key.contains("."))
				.map(key -> key.substring(0,key.indexOf("."))).distinct().collect(Collectors.toList());
		Map<String, ArrayNode> cacheArrays = new HashMap<>();
 		for (String key : objPrimerNivel) {
 			log.debug("creates COMPLEX OBJECT: {} " + key + "- prefix: {}", key, prefix);
 			//if it is an array...
 			if (key.endsWith("]")){
 				String claveArray = key.substring(0, key.indexOf("["));
 				//create an array node for the key... 
 				ArrayNode arrayNode = cacheArrays.get(claveArray);
 				if (arrayNode == null){
 					arrayNode = mapper.createArrayNode();
 					cacheArrays.put(claveArray, arrayNode);
 					padre.set(claveArray, arrayNode);
 				} 
				arrayNode.add(createNode(convertFrom, keys, prefix, key));
 			} else {
 				//caso contrario es un objeto
 				padre.set(key, createNode(convertFrom, keys, prefix, key));
 			}
		}
		return objPrimerNivel;
	}

	private ObjectNode createNode(Map<String, Object> convertFrom, Set<String> keys, String prefix, String key) {
		ObjectNode primerNivelObj = mapper.createObjectNode();
		log.debug("creates NODE OBJECT: {} - prefix: {}", key, prefix);
		Set<String> objSegundoNivel = keys.stream().filter(p -> p.startsWith(key + "."))
				.map(p -> p.substring((key + ".").length(),p.length())).distinct().collect(Collectors.toSet());
		
		creaObjetosSimples(convertFrom, primerNivelObj, objSegundoNivel, prefix + key + ".");
		creaObjetosComplejos(convertFrom, objSegundoNivel, primerNivelObj, prefix + key + ".");
		
		return primerNivelObj;
	}

	private List<String> creaObjetosSimples(Map<String, Object> convertFrom, ObjectNode tramite,
			Set<String> keys, String prefix) {
		
		log.debug("creates SIMPLE OBJECT prefix: ", prefix);
		List<String> primerNivelSimpleKeys = keys.stream().filter(key -> !key.contains(".")).distinct().collect(Collectors.toList());
		for (String key: primerNivelSimpleKeys) {
			
			Object value = convertFrom.get(prefix + key);
			log.debug("Converting: " + prefix + key + " -> " + value);
			convertFrom.remove(prefix + key);
			
			if (value != null){
				if (value instanceof String){
					String v = (String) value;
					if (v.startsWith("\"") && v.endsWith("\"")){
						value = v.substring(1, v.length() -1);
						tramite.put(key, (String)value);
					} else if (v.startsWith("{{") && v.endsWith("}}")){
						//if the expression contains: {{ }}...
						String expressionValue = v.substring(2, v.length() -2);
						if (!expressionValue.isEmpty()){
							//get the function to apply eg. {{RANDOM(1,200)}}
							int index = expressionValue.indexOf("(");
							if (index < 0){
								log.error("Unrecognized expression type, assigned as text");
								tramite.put(key, (String)value);
							} else {
								String function = expressionValue.substring(0, index);
								if (function.isEmpty()){
									log.error("Unrecognized function type, assigned as text");
									tramite.put(key, (String)value);
								} else {
									Function<String[],Object> f = functionMap.get(function);
									if (f == null){
										log.error("Function {} not found, assigned as text", function);
										tramite.put(key, (String)value);
									} else {
										int lastIndex = expressionValue.lastIndexOf(")");
										if (lastIndex > index){
											String params = expressionValue.substring(index+1, lastIndex);
											Object fResult = f.apply(params.split(","));
											if (fResult == null){
												log.warn("Function {} returned a null value", function);
											} if (fResult instanceof String){
												tramite.put(key, (String) fResult);
											} else if (fResult instanceof Integer){
												tramite.put(key, (Integer) fResult);
											} else if (fResult instanceof Long){
												tramite.put(key, (Long) fResult);
											} else if (fResult instanceof Double){
												tramite.put(key, (Double) fResult);
											} else if (fResult instanceof Float){
												tramite.put(key, (Float) fResult);
											} else if (fResult instanceof BigDecimal){
												tramite.put(key, (BigDecimal) fResult);
											} else {
												log.error("Unrecognized return type for function {}, assigned as text", function);
												tramite.put(key, (String)value);
											}
										} else {
											log.error("Failed to executed function {}, assigned as text", function);
											tramite.put(key, (String)value);
										}
									}
								}
							}
						} else {
							tramite.put(key, (String)value);
						}
					} else {
						String vt = v.trim();
						if (vt.trim().equalsIgnoreCase("true") || vt.trim().equalsIgnoreCase("false")){
							tramite.put(key, (Boolean) Boolean.valueOf(vt));
						} else {
							try {
								Integer i = Integer.valueOf(vt);
								tramite.put(key, (Integer) i);
							} catch (NumberFormatException  e){
								try {
									Long l = Long.valueOf(vt);
									tramite.put(key, (Long) l);
								} catch (NumberFormatException  e2){
									try {
										Float f = new Float(vt);
										tramite.put(key, (Float) f);
									} catch (NumberFormatException  e3){
										try {
											Double d = new Double(vt);
											tramite.put(key, (Double) d);
										} catch (NumberFormatException  e4){
											try {
												BigDecimal bd = new BigDecimal(vt);
												tramite.put(key, (BigDecimal) bd);
											} catch (NumberFormatException  e5){
												log.error("Unrecognized data type for key {}, assigned as text", key);
												tramite.put(key, (String)value);
											}
										}	
									}	
								}	
							}
						}
					
					}
				} else if (value instanceof Integer){
					tramite.put(key, (Integer) value);
				} else if (value instanceof Long){
					tramite.put(key, (Long) value);
				} else if (value instanceof Double){
					tramite.put(key, (Double) value);
				} else if (value instanceof Float){
					tramite.put(key, (Float) value);
				} else if (value instanceof BigDecimal){
					tramite.put(key, (BigDecimal) value);
				}
			}
		}
		return primerNivelSimpleKeys;
	}

}
