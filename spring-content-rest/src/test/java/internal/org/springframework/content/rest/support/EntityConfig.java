package internal.org.springframework.content.rest.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Configuration
public class EntityConfig {

    @RepositoryRestController
    public class EntityController {

        @Autowired
        private TestEntity3Repository repo;

        @RequestMapping(value = "/testEntity3s/{id}/softDelete", method = RequestMethod.DELETE)
        public void softDelete(@PathVariable("id") Long id) {
            TestEntity3 te3 = repo.getById(id);
            // set delete flag
            int i=0;
        }
    }
}
