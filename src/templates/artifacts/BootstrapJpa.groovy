import javax.persistence.EntityManager

class BootstrapJpa {
    def init = { String persistenceUnit, EntityManager em ->
    }

    def destroy = { String persistenceUnit, EntityManager em ->
    }
} 
