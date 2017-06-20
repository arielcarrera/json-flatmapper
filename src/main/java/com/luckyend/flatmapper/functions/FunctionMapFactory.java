package com.luckyend.flatmapper.functions;

import java.util.Map;
import java.util.function.Function;

public interface FunctionMapFactory {

	/**
	 * Returns a map of functions
	 */
	Map<String, Function<String[], Object>> getFunctionMap();

}