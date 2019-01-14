package org.apache.ibatis.submitted.cache;

import org.apache.ibatis.annotations.CacheNamespaceRef;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Options.FlushCachePolicy;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@CacheNamespaceRef(PersonMapper.class) // by type
public interface ImportantPersonMapper {

	@Select("select id, firstname, lastname from person")
	@Options(flushCache = FlushCachePolicy.TRUE)
	List<Person> findWithFlushCache();

}
