"""
Agent 2b — Action Router.

Deterministic (no LLM) action_key → Angular route mapping. When guardrails
classifies an INDIVIDUAL user's message as `action_redirect`, this node emits
a final_response + redirect URL so the frontend can render a CTA button.
"""
from graph.nodes._logging import log_event
from graph.state import AgentState


_ACTION_CATALOG = {
    "view_orders": {
        "url": "/orders",
        "label": {"tr": "Siparişlerime Git", "en": "View My Orders"},
        "msg_template": {
            "tr": "Sipariş geçmişine bakmak için [buraya tıklayınız]({url}).",
            "en": "You can [click here]({url}) to view your order history.",
        },
    },
    "start_return": {
        "url": "/returns",
        "label": {"tr": "İade Başlat", "en": "Start a Return"},
        "msg_template": {
            "tr": "İade talebi oluşturmak için [buraya tıklayınız]({url}).",
            "en": "You can [click here]({url}) to start a return.",
        },
    },
    "view_cart": {
        "url": "/cart",
        "label": {"tr": "Sepete Git", "en": "View Cart"},
        "msg_template": {
            "tr": "Sepetine göz atmak için [buraya tıklayınız]({url}).",
            "en": "You can [click here]({url}) to view your cart.",
        },
    },
    "view_wishlist": {
        "url": "/wishlist",
        "label": {"tr": "Favorilerime Git", "en": "View Wishlist"},
        "msg_template": {
            "tr": "Favorilerini görüntülemek için [buraya tıklayınız]({url}).",
            "en": "You can [click here]({url}) to view your wishlist.",
        },
    },
    "view_addresses": {
        "url": "/addresses",
        "label": {"tr": "Adreslerim", "en": "Manage Addresses"},
        "msg_template": {
            "tr": "Adreslerini yönetmek için [buraya tıklayınız]({url}).",
            "en": "You can [click here]({url}) to manage your addresses.",
        },
    },
    "view_profile": {
        "url": "/profile",
        "label": {"tr": "Profilim", "en": "View Profile"},
        "msg_template": {
            "tr": "Profil sayfana gitmek için [buraya tıklayınız]({url}).",
            "en": "You can [click here]({url}) to view your profile.",
        },
    },
}


def action_router_node(state: AgentState) -> AgentState:
    action_key = state.get("action_key")
    lang = state.get("language") or "en"
    if lang not in ("tr", "en"):
        lang = "en"
    spec = _ACTION_CATALOG.get(action_key or "")
    user_context = state.get("user_context") or {}

    if spec is None:
        log_event(
            "action_router.unknown_action",
            level="warning",
            session_id=state.get("session_id"),
            user_id=user_context.get("user_id"),
            role=user_context.get("role"),
            action_key=action_key,
        )
        fallback = (
            "Tam olarak hangi sayfayı istediğini anlayamadım. "
            "Siparişler, sepet, favoriler, iade, adresler veya profil arasından birini dener misin?"
            if lang == "tr"
            else "I'm not sure which page you want. "
                 "Try: orders, cart, wishlist, returns, addresses, or profile."
        )
        return {
            **state,
            "redirect_url": None,
            "redirect_label": None,
            "final_response": fallback,
        }

    log_event(
        "action_router.redirected",
        session_id=state.get("session_id"),
        user_id=user_context.get("user_id"),
        role=user_context.get("role"),
        action_key=action_key,
        redirect_url=spec["url"],
        language=lang,
    )
    return {
        **state,
        "redirect_url": spec["url"],
        "redirect_label": spec["label"][lang],
        "final_response": spec["msg_template"][lang].format(url=spec["url"]),
    }
