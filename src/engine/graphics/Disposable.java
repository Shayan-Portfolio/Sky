package engine.graphics;

import engine.util.Internal;
import engine.logging.SkyRuntimeException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class Disposable {
    public Disposable disposer;
    public List<Disposable> childrenToDispose = new ArrayList<>();

    public Disposable(Disposable parent){
        this.disposer = parent;
        if(parent != null) {
            parent.addDisposable(this);
        }
    }


    public void removeDisposable(Disposable child) {
        for (Iterator<Disposable> iterator = childrenToDispose.iterator(); iterator.hasNext(); ) {
            Disposable ref = iterator.next();
            if (ref == child) {
                iterator.remove();
            }
        }
    }

    public void addDisposable(Disposable child){

        if(child == this) {
            throw new SkyRuntimeException("A Disposable cannot be a child of itself");
        }
        child.disposer = this;
        childrenToDispose.add(child);
    }


    public List<Disposable> getChildrenToDispose() {
        return childrenToDispose;
    }

    public void disposeAll(){
        disposeRecursive(this);
    }

    public void disposeRecursive(Disposable disposable){

        String string = (getDepthString(disposable)+ " " + disposable.getClass().getSimpleName());
        System.out.println(string);
        for(Disposable child : disposable.childrenToDispose) {
            disposeRecursive(child);
        }

        disposable.dispose();
    }


    @Internal("Game code should never call dispose() directly, " +
            "it is always safer to use disposeAll() instead " +
            "to ensure proper resource disposal ordering")
    public abstract void dispose();

    private String getDepthString(Disposable ref){

        int i = 0;

        while(ref.disposer != null){
            ref = ref.disposer;
            i++;
        }

        StringBuilder stringBuilder = new StringBuilder();
        for (int j = 0; j < i; j++) {
            if(j == i - 1) stringBuilder.append("|");
            else stringBuilder.append("  ");
        }

        return stringBuilder.toString();
    }



}
