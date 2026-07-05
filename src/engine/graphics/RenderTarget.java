package engine.graphics;

import java.util.ArrayList;
import java.util.List;

public class RenderTarget extends Disposable {

    private ArrayList<Attachment> attachments;




    public RenderTarget(Disposable parent) {
        super(parent);
        attachments = new ArrayList<>();
    }

    public void addAttachment(Attachment attachment) {
        attachments.add(attachment);
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public Attachment getAttachmentByIndex(int index) {
        return attachments.get(index);
    }

    public int getAttachmentCountExcludingDepth() {
        int attachmentCount = 0;
        for(Attachment attachment : getAttachments()) {
            if((attachment.getFlags() & AttachmentTypes.Depth) == 0)
                attachmentCount++;
        }
        return attachmentCount;
    }

    public Attachment getAttachment(long mask) {
        for(Attachment attachment : attachments) {
            if((attachment.getFlags() & mask) != 0) return attachment;
        }

        return null;
    }



    @Override
    public void dispose() {

    }
}
