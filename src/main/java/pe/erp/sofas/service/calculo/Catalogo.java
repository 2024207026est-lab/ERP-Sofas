package pe.erp.sofas.service.calculo;

import pe.erp.sofas.model.FichaTecnica;
import pe.erp.sofas.model.Material;

/** Lo que la explosion necesita consultar: fichas aprobadas (para las compuestas) y materiales. */

public interface Catalogo {
    
    /** Version aprobada de la ficha con ese codigo, o null. */
    
    FichaTecnica fichaAprobada(String codigo);

    
    Material material(String codigo);
}
