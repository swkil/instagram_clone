import { useEffect } from "react";
import useCommentStore from "../../store/commentStore";
import CommentItem from "./CommentItem";

const CommentList = ({ postId }) => {
    const { fetchComments, comments, error } = useCommentStore();

    const postComments = comments[postId] || [];

    useEffect(() => {
        const getComments = async () => {
            try {
                await fetchComments(postId);
            } catch (err) {
                console.error(err);
            }
        };

        if (postId) {
            getComments();
        }
    }, [postId, fetchComments]);

    if (error) {
        return <p className="text-red-500 text-sm">{error}</p>;
    }

    return (
        <div>
            {postComments.length > 0 ? (
                postComments.map((comment) => (
                    <CommentItem key={comment.id} comment={comment} />
                ))
            ) : (
                <p className="text-gray-500 text-sm">No comments yet.</p>
            )}
        </div>
    );
};

export default CommentList;